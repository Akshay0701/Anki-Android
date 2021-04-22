package com.ichi2.anki;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ichi2.anki.analytics.AnalyticsDialogFragment;
import com.ichi2.anki.dialogs.DeckSelectionDialog;
import com.ichi2.anki.web.HostNumFactory;
import com.ichi2.async.Connection;
import com.ichi2.libanki.Utils;
import com.ichi2.preferences.PreferenceExtensions;
import com.ichi2.themes.StyledProgressDialog;
import com.ichi2.ui.FixedEditText;
import com.ichi2.utils.FilterResultsUtils;
import com.ichi2.utils.FunctionalInterfaces;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static com.ichi2.anim.ActivityTransitionAnimation.Direction.FADE;
import static com.ichi2.anim.ActivityTransitionAnimation.Direction.START;

public class SwtichProfileDialog {

    private MaterialDialog.Builder mBuilder;
    private Context mContext;
    private  SharedPreferences mPrefs;
    private SwtichProfileDialog.ProfilesArrayAdapter mAdapter;
    private final String mSHARED_PREFS_ID = "SwtichProfile";

    /** work done till now
     * 1 created list of profile
     * 2 add profile
     * 3 search profile // todo need more work
     * 4 onClick of profile to goes to login page and to following computation
     *   -> if selected profile is the current profile then it will simple navigate to home page by message "Already logged In"
     *   -> else if selected profile has login credentials it will directly login
     *   -> else selected profile has no credentials then user have to login and then that credentials will store to list and page will navigate to home
     * 5 After successfully switch, Dickpicker activity will restart with new collection path
     */

    public SwtichProfileDialog(@NonNull Context context) {
        this.mContext = context;
        mPrefs = mContext.getSharedPreferences(mSHARED_PREFS_ID, Context.MODE_PRIVATE);
    }

    public void showSwtichProfileDialog() {

        View dialogView = LayoutInflater.from(mContext)
                .inflate(R.layout.deck_picker_dialog, null, false);

        TextView summary = dialogView.findViewById(R.id.deck_picker_dialog_summary);
        summary.setVisibility(View.GONE);

        RecyclerView recyclerView = dialogView.findViewById(R.id.deck_picker_dialog_list);
        recyclerView.requestFocus();

        RecyclerView.LayoutManager deckLayoutManager = new LinearLayoutManager(mContext);
        recyclerView.setLayoutManager(deckLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        HashMap<String, Profile> profiles = getProfileList();
        mAdapter = new SwtichProfileDialog.ProfilesArrayAdapter(profiles, profiles.values());
        recyclerView.setAdapter(mAdapter);

        adjustToolbar(dialogView, mAdapter);

        mBuilder = new MaterialDialog.Builder(mContext);

        mBuilder.neutralText(R.string.dialog_cancel)
                .customView(dialogView, false).show();
    }

    public HashMap<String, Profile> getProfileList() {

        // creating a variable for gson.
        Gson gson = new Gson();

        // below line is to get to string present from our
        // shared prefs if not present setting it as null.
        String json = mPrefs.getString("profilesList", null);

        // below line is to get the type of our array list.
        Type type = new TypeToken<HashMap<String, Profile>>() {}.getType();

        // in below line we are getting data from gson
        // and saving it to our array list
        HashMap<String, Profile> profiles = gson.fromJson(json, type);

        // checking below if the array list is empty or not
        if (profiles == null) {
            // if the array list is empty
            // creating a new array list.
            profiles = new HashMap<>();
            profiles.put("AnkiDroid",new Profile("AnkiDroid", "", ""));
        }

        // return parsed list
        return profiles;
    }

    public void saveList(HashMap<String, Profile> profiles) {

        SharedPreferences.Editor editor = mPrefs.edit();

        // creating a new variable for gson.
        Gson gson = new Gson();

        // getting data from gson and storing it in a string.
        String json = gson.toJson(profiles);

        // below line is to save data in shared
        // prefs in the form of string.
        editor.putString("profilesList", json);

        // below line is to apply changes
        // and save data in shared prefs.
        editor.apply();

    }

    private void adjustToolbar(View dialogView, SwtichProfileDialog.ProfilesArrayAdapter adapter) {
        Toolbar mToolbar = dialogView.findViewById(R.id.deck_picker_dialog_toolbar);

        mToolbar.setTitle("Users Profiles");

        mToolbar.inflateMenu(R.menu.deck_picker_dialog_menu);

        MenuItem searchItem = mToolbar.getMenu().findItem(R.id.deck_picker_dialog_action_filter);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setQueryHint(mContext.getString(R.string.deck_picker_dialog_filter_decks));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });

        MenuItem addDecks = mToolbar.getMenu().findItem(R.id.deck_picker_dialog_action_add_deck);
        addDecks.setOnMenuItemClickListener(menuItem -> {
            // creating new deck without any parent deck
            addProfileDialog();
            return true;
        });

    }

    /**
     * A dialog which handles creation of new profile
     */
    private void addProfileDialog() {
        EditText mDialogEditText = new FixedEditText(mContext);
        new MaterialDialog.Builder(mContext)
                .title("Create Profile")
                .positiveText(R.string.dialog_ok)
                .customView(mDialogEditText, true)
                .negativeText(R.string.dialog_cancel)
                .onPositive((dialog, which) -> addProfiletoList(mDialogEditText.getText().toString()))
                .show();
    }

    private void addProfiletoList(String newProfileName) {
        if (!newProfileName.isEmpty()) {
            Profile profile = new Profile(newProfileName, "", "");
            mAdapter.mProfileArrayList.put(profile.getUserName(), profile);
            saveList(mAdapter.mProfileArrayList);
            selectProfileAndClose(profile);
        }
    }

    protected void onProfileSelected(@NonNull Profile profile) {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(mContext);
        String username = preferences.getString("deckPath", "/storage/emulated/0/AnkiDroid").substring(20);
        if (profile.getUserName().equals(username)){
            // no change
            UIUtils.showThemedToast(mContext,"selected already logged profile",true);
        } else if (profile.getEmail().isEmpty() && profile.getPassword().isEmpty()) {
            // create new profile
            createNewProfile(profile.getUserName());
        } else {
            // profile has login info, so direct log in to account
            preferences.edit().putString("deckPath", "/storage/emulated/0/" + profile.getUserName()).apply();
            Intent intent = new Intent(mContext,MyAccount.class);
            intent.putExtra("profile", (Serializable) profile);
            mContext.startActivity(intent);
        }
    }

    private void createNewProfile(String profileName) {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(mContext);
        preferences.edit().putString("deckPath", "/storage/emulated/0/" + profileName).apply();
        // logout
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("username", "");
        editor.putString("hkey", "");
        editor.apply();
        HostNumFactory.getInstance(mContext).reset();
        //  force media resync on deauth
        new AnkiActivity().getCol().getMedia().forceResync();
        // restart
        restartWithNewDeckPicker();
    }

    protected void restartWithNewDeckPicker() {
        // PERF: DB access on foreground thread
        CollectionHelper.getInstance().closeCollection(true, "Preference Modification: collection path changed");
        Intent deckPicker = new Intent(mContext, DeckPicker.class);
        deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(deckPicker);
    }

    protected void deleteSeletedProfile(String profileName) {
        new MaterialDialog.Builder(mContext)
                .title("Delete "+ profileName)
                .positiveText(R.string.dialog_ok)
                .negativeText(R.string.dialog_cancel)
                .onPositive((dialog, which) -> {
                    mAdapter.mProfileArrayList.remove(profileName);
                    saveList(mAdapter.mProfileArrayList);
                    mBuilder.show().dismiss();
                    // check if user deleting current profile
                    SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(mContext);
                    if (preferences.getString("deckPath","Default").substring(20).equals(profileName)) {
                        preferences.edit().putString("deckPath", "/storage/emulated/0/AnkiDroid").apply();
                        Intent intent = new Intent(mContext,MyAccount.class);
                        intent.putExtra("profile", (Parcelable) mAdapter.mProfileArrayList.get("Default"));
                        mContext.startActivity(intent);
                    }
                })
                .show();
    }

    protected void selectProfileAndClose(@NonNull SwtichProfileDialog.Profile profile) {
        onProfileSelected(profile);
    }

    public class ProfilesArrayAdapter extends RecyclerView.Adapter<SwtichProfileDialog.ProfilesArrayAdapter.ViewHolder> implements Filterable {
        public class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView mDeckTextView;
            public ViewHolder(@NonNull TextView ctv) {
                super(ctv);
                mDeckTextView = ctv;
                mDeckTextView.setOnClickListener(view -> {
                    String profileName = ctv.getText().toString();
                    selectProfileByNameAndClose(profileName);
                });

                mDeckTextView.setOnLongClickListener(view -> {
                    String profileName = ctv.getText().toString();
                    if (!profileName.equals("AnkiDroid")) {
                        deleteSeletedProfile(profileName);
                    } else {
                        UIUtils.showThemedToast(mContext,R.string.default_conf_delete_error,true);
                    }
                    return true;
                });
            }

            public void setDeck(@NonNull SwtichProfileDialog.Profile profile) {
                mDeckTextView.setText(profile.getUserName());
            }
        }

        private final HashMap<String, Profile> mProfileArrayList = new HashMap<>();
        private final ArrayList<Profile> mValueSet = new ArrayList<>();

        public ProfilesArrayAdapter(@NonNull HashMap<String, Profile> profileArrayList, @NonNull Collection<Profile> valueSet) {
            mProfileArrayList.putAll(profileArrayList);
            mValueSet.addAll(valueSet);
        }

        @SuppressLint("DirectToastMakeTextUsage")
        protected void selectProfileByNameAndClose(@NonNull String deckName) {
            for (Map.Entry<String, Profile> p : mProfileArrayList.entrySet()) {
                if (p.getValue().getUserName().equals(deckName)) {
                    selectProfileAndClose(p.getValue());
                    return;
                }
            }
        }

        @NonNull
        @Override
        public SwtichProfileDialog.ProfilesArrayAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.deck_picker_dialog_list_item, parent, false);

            return new SwtichProfileDialog.ProfilesArrayAdapter.ViewHolder(v.findViewById(R.id.deck_picker_dialog_list_item_value));
        }

        @Override
        public void onBindViewHolder(@NonNull SwtichProfileDialog.ProfilesArrayAdapter.ViewHolder holder, int position) {
            SwtichProfileDialog.Profile profile = mValueSet.get(position);
            holder.setDeck(profile);
        }

        @Override
        public int getItemCount() {
            return mProfileArrayList.size();
        }


        @NonNull
        @Override
        public Filter getFilter() {
            return new SwtichProfileDialog.ProfilesArrayAdapter.DecksFilter();
        }

        private class DecksFilter extends Filter {
            private final HashMap<String, Profile> mProfileArrayList;
            protected DecksFilter() {
                super();
                mProfileArrayList = new HashMap<>();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                mProfileArrayList.clear();
                HashMap<String, Profile> allDecks = SwtichProfileDialog.ProfilesArrayAdapter.this.mProfileArrayList;
                if (constraint.length() == 0) {
                    mProfileArrayList.putAll(allDecks);
                } else {
                    final String filterPattern = constraint.toString().toLowerCase(Locale.getDefault()).trim();
                    for (Map.Entry<String, Profile> profile : allDecks.entrySet()) {
                        if (profile.getValue().getUserName().toLowerCase(Locale.getDefault()).contains(filterPattern)) {
                            mProfileArrayList.put(profile.getKey(), profile.getValue());
                        }
                    }
                }
                // todo this need review and work
                return FilterResultsUtils.fromCollection(mProfileArrayList.keySet());
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                HashMap<String, Profile> currentlyProfiles = SwtichProfileDialog.ProfilesArrayAdapter.this.mProfileArrayList;
                currentlyProfiles.clear();
                currentlyProfiles.putAll(mProfileArrayList);
//                Collections.sort(currentlyProfiles);
                notifyDataSetChanged();
            }
        }
    }


    public static class Profile implements Comparable<SwtichProfileDialog.Profile>, Parcelable, Serializable {
        private String mUserName;
        private String mEmail;
        private String mPassword;


        public Profile(Parcel in) {
            this.mEmail = in.readString();
            this.mUserName = in.readString();
            this.mPassword = in.readString();
        }

        public void setUserName(String userName) {
            mUserName = userName;
        }


        public void setEmail(String email) {
            mEmail = email;
        }


        public void setPassword(String password) {
            mPassword = password;
        }


        public String getUserName() {
            return mUserName;
        }


        public String getEmail() {
            return mEmail;
        }


        public String getPassword() {
            return mPassword;
        }

        public Profile(String userName, String email, String password) {
            this.mEmail = email;
            this.mUserName = userName;
            this.mPassword = password;
        }


        @Override
        public int compareTo(@NonNull SwtichProfileDialog.Profile o) {
            return this.mUserName.compareTo(o.mUserName);
        }


        @Override
        public int describeContents() {
            return 0;
        }


        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeString(mEmail);
            dest.writeString(mUserName);
            dest.writeString(mPassword);
        }


        @SuppressWarnings("unused")
        public static final Parcelable.Creator<Profile> CREATOR = new Parcelable.Creator<Profile>() {
            @Override
            public Profile createFromParcel(Parcel in) {
                return new Profile(in);
            }


            @Override
            public Profile[] newArray(int size) {
                return new Profile[size];
            }
        };
    }
}