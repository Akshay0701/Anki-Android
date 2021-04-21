package com.ichi2.anki.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.ichi2.anki.R;
import com.ichi2.anki.SwtichProfileDialog;

import java.util.ArrayList;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SwtichUserDialog {
    private final MaterialDialog.Builder mBuilder;
    private Context mContext;
    private SharedPreferences mPrefs;

    public SwtichUserDialog(Context context) {
        mContext = context;
        mBuilder = new MaterialDialog.Builder(context);
        View dialogView = LayoutInflater.from(mContext)
                .inflate(R.layout.deck_picker_dialog, null, false);


//        Bundle arguments = requireArguments();

        mPrefs = mContext.getSharedPreferences("SwtichProfile", Context.MODE_PRIVATE);


        RecyclerView recyclerView = dialogView.findViewById(R.id.deck_picker_dialog_list);
        recyclerView.requestFocus();

        RecyclerView.LayoutManager deckLayoutManager = new LinearLayoutManager(mContext);
        recyclerView.setLayoutManager(deckLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        ArrayList<SwtichProfileDialog.Profile> profiles = new ArrayList<>();
//        mAdapter = new SwtichProfileDialog.ProfilesArrayAdapter(profiles);
//        recyclerView.setAdapter(mAdapter);

//        adjustToolbar(dialogView, mAdapter);

        mBuilder.neutralText(R.string.dialog_cancel)
                .customView(dialogView, false).show();
    }


}
