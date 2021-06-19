package com.ichi2.anki.multimediacard.visualeditor;


import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;

import com.ichi2.anki.R;
import com.ichi2.anki.UIUtils;
import com.ichi2.anki.multimediacard.IMultimediaEditableNote;
import com.ichi2.anki.multimediacard.activity.MultimediaEditFieldActivity;
import com.ichi2.anki.multimediacard.activity.VisualEditorActivity;
import com.ichi2.anki.multimediacard.fields.IField;
import com.ichi2.anki.multimediacard.fields.ImageField;
import com.ichi2.anki.servicelayer.NoteService;
import com.ichi2.libanki.Note;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.TooltipCompat;
import timber.log.Timber;

import static com.ichi2.anki.NoteEditor.REQUEST_MULTIMEDIA_EDIT;
import static com.ichi2.anki.multimediacard.visualeditor.VisualEditorFunctionality.ALIGN_CENTER;
import static com.ichi2.anki.multimediacard.visualeditor.VisualEditorFunctionality.ALIGN_JUSTIFY;
import static com.ichi2.anki.multimediacard.visualeditor.VisualEditorFunctionality.ALIGN_LEFT;
import static com.ichi2.anki.multimediacard.visualeditor.VisualEditorFunctionality.ALIGN_RIGHT;
import static com.ichi2.anki.multimediacard.visualeditor.VisualEditorFunctionality.BOLD;
import static com.ichi2.anki.multimediacard.visualeditor.VisualEditorFunctionality.CLEAR_FORMATTING;
import static com.ichi2.anki.multimediacard.visualeditor.VisualEditorFunctionality.EDIT_SOURCE;
import static com.ichi2.anki.multimediacard.visualeditor.VisualEditorFunctionality.HORIZONTAL_RULE;
import static com.ichi2.anki.multimediacard.visualeditor.VisualEditorFunctionality.ITALIC;
import static com.ichi2.anki.multimediacard.visualeditor.VisualEditorFunctionality.ORDERED_LIST;
import static com.ichi2.anki.multimediacard.visualeditor.VisualEditorFunctionality.UNDERLINE;
import static com.ichi2.anki.multimediacard.visualeditor.VisualEditorFunctionality.UNORDERED_LIST;

// currently only used for adding new view to linearLayout of VisualEditorToolbar
public class VisualEditorToolbar extends LinearLayoutCompat {

    VisualEditorActivity mVisualEditorActivity;
    VisualEditorWebView mWebView;

    public VisualEditorToolbar(Context context) {
        super(context);
    }

    public VisualEditorToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VisualEditorToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setupEditorScrollbarButtons(VisualEditorActivity ankiActivity, VisualEditorWebView webView) {
        mWebView = webView;
        mVisualEditorActivity = ankiActivity;

        SimpleListenerSetup setupAction = (id, functionality, tooltipStringResource) -> {
            String jsName = mWebView.getJsFunctionName(functionality);
            if (jsName == null) {
                Timber.d("Skipping functionality: %s", functionality);
            }
            View view = findViewById(id);
            view.setOnClickListener(v -> mWebView.execFunction(jsName));
            setTooltip(view, mVisualEditorActivity.getString(tooltipStringResource));
        };

        setupAction.apply(R.id.editor_button_bold, BOLD, R.string.visual_editor_tooltip_bold);
        setupAction.apply(R.id.editor_button_italic, ITALIC, R.string.visual_editor_tooltip_italic);
        setupAction.apply(R.id.editor_button_underline, UNDERLINE, R.string.visual_editor_tooltip_underline);
        setupAction.apply(R.id.editor_button_clear_formatting, CLEAR_FORMATTING, R.string.visual_editor_tooltip_clear_formatting);
        setupAction.apply(R.id.editor_button_list_bullet, UNORDERED_LIST, R.string.visual_editor_tooltip_bullet_list);
        setupAction.apply(R.id.editor_button_list_numbered, ORDERED_LIST, R.string.visual_editor_tooltip_numbered_list);
        setupAction.apply(R.id.editor_button_horizontal_rule, HORIZONTAL_RULE, R.string.visual_editor_tooltip_horizontal_line);
        setupAction.apply(R.id.editor_button_align_left, ALIGN_LEFT, R.string.visual_editor_tooltip_horizontal_line);
        setupAction.apply(R.id.editor_button_align_center, ALIGN_CENTER, R.string.visual_editor_tooltip_horizontal_line);
        setupAction.apply(R.id.editor_button_align_right, ALIGN_RIGHT, R.string.visual_editor_tooltip_horizontal_line);
        setupAction.apply(R.id.editor_button_align_justify, ALIGN_JUSTIFY, R.string.visual_editor_tooltip_horizontal_line);
        setupAction.apply(R.id.editor_button_view_html, EDIT_SOURCE, R.string.visual_editor_edit_html); //this is a toggle.

        AndroidListenerSetup setupAndroidListener = (id, function, tooltipId) -> {
            View view = findViewById(id);
            view.setOnClickListener(v -> function.run());
            setTooltip(view, getResources().getString(tooltipId));
        };

        setupAndroidListener.apply(R.id.editor_button_cloze, this::performCloze, R.string.visual_editor_tooltip_cloze);
        setupAndroidListener.apply(R.id.editor_button_add_image, this::openAdvancedViewerForAddImage, R.string.visual_editor_tooltip_add_image);

    }


    private void openAdvancedViewerForAddImage() {
        if (!checkCollectionHasLoaded(R.string.visual_editor_could_not_start_add_image)) {
            return;
        }

        IField field = new ImageField();
        openMultimediaEditor(field);
    }

    private void openMultimediaEditor(IField field) {
        IMultimediaEditableNote note = NoteService.createEmptyNote(Objects.requireNonNull(mVisualEditorActivity.getCol().getModels().get(mVisualEditorActivity.getModelId())));
        if (note != null) {
            note.setField(0, field);
            Intent editCard = new Intent(mVisualEditorActivity, MultimediaEditFieldActivity.class);
            editCard.putExtra(MultimediaEditFieldActivity.EXTRA_FIELD_INDEX, 0);
            editCard.putExtra(MultimediaEditFieldActivity.EXTRA_FIELD, field);
            editCard.putExtra(MultimediaEditFieldActivity.EXTRA_WHOLE_NOTE, note);
            mVisualEditorActivity.startActivityForResultWithoutAnimation(editCard, REQUEST_MULTIMEDIA_EDIT);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean checkCollectionHasLoaded(@StringRes int resId) {
        if (mVisualEditorActivity.getModelId() == 0 || !mVisualEditorActivity.isHasLoadedCol()) {
            //Haven't loaded yet.
            UIUtils.showThemedToast(mVisualEditorActivity, mVisualEditorActivity.getString(resId), false);
            return false;
        }
        return true;
    }

    private void performCloze() {
        mWebView.insertCloze(getNextClozeId());
    }

    private int getNextClozeId() {
        List<String> fields = Arrays.asList(mVisualEditorActivity.getFields());
        fields.set(mVisualEditorActivity.getIndex(), mVisualEditorActivity.getCurrentText());
        return Note.ClozeUtils.getNextClozeIndex(fields);
    }


    private void setTooltip(View view, String tooltip) {
        TooltipCompat.setTooltipText(view, tooltip);
    }

    /** Setup a button which executes a JavaScript runnable */
    @FunctionalInterface
    protected interface SimpleListenerSetup {
        void apply(@IdRes int buttonId, VisualEditorFunctionality function, @StringRes int tooltipText);
    }

    /** A button which performs an Android Runnable */
    @FunctionalInterface
    protected interface AndroidListenerSetup {
        void apply(@IdRes int buttonId, Runnable function, @StringRes int tooltipText);
    }

}