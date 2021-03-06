package com.vsklamm.cppquiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.pddstudio.highlightjs.HighlightJsView;
import com.pddstudio.highlightjs.models.Language;
import com.pddstudio.highlightjs.models.Theme;
import com.vsklamm.cppquiz.model.Question;
import com.vsklamm.cppquiz.model.ResultBehaviourType;

import ru.noties.markwon.Markwon;

import static com.vsklamm.cppquiz.MainActivity.APP_PREFERENCES;
import static com.vsklamm.cppquiz.MainActivity.APP_PREF_LINE_NUMBERS;
import static com.vsklamm.cppquiz.MainActivity.APP_PREF_ZOOM;
import static com.vsklamm.cppquiz.MainActivity.IS_GIVE_UP;
import static com.vsklamm.cppquiz.MainActivity.QUESTION;
import static com.vsklamm.cppquiz.MainActivity.THEME;

public class ExplanationActivity extends AppCompatActivity {

    HighlightJsView codeView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explanation);

        Intent intent = getIntent();

        final boolean isGiveUp = intent.getBooleanExtra(IS_GIVE_UP, true);
        final Question question = (Question) intent.getSerializableExtra(QUESTION);

        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            String title;
            if (isGiveUp) {
                title = String.format(getResources().getString(R.string.toolbar_give_up), question.getId());
            } else {
                title = String.format(getResources().getString(R.string.toolbar_correct), question.getId());
            }
            ab.setTitle(title);
        }

        SharedPreferences appPreferences = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);

        final String theme = appPreferences.getString(THEME, "GITHUB");
        codeView = findViewById(R.id.highlight_view_card_view_2);
        codeView.setTheme(Theme.valueOf(theme));
        codeView.setHighlightLanguage(Language.C_PLUS_PLUS);
        codeView.setSource(question.getCode());

        onZoomSupportToggled(appPreferences.getBoolean(APP_PREF_ZOOM, false));
        onShowLineNumbersToggled(appPreferences.getBoolean(APP_PREF_LINE_NUMBERS, false));

        TextView tvAnswer = findViewById(R.id.tv_answer);
        TextView tvExplanation = findViewById(R.id.tv_explanation);

        String[] results = getResources().getStringArray(R.array.result_behaviour_type);
        String resultText = results[question.getResult().ordinal()];
        String answerText = (question.getResult() == ResultBehaviourType.OK) ? (" `" + question.getAnswer() + "`") : "";

        int color;
        if (isGiveUp) {
            color = ContextCompat.getColor(this, R.color.git_background);
        } else {
            color = ContextCompat.getColor(this, R.color.explanation_correct);
        }
        tvAnswer.setBackgroundColor(color);
        tvExplanation.setBackgroundColor(color);

        Markwon.setMarkdown(tvAnswer, resultText + answerText);
        Markwon.setMarkdown(tvExplanation, question.getExplanation());

        Button btnNextQuestion = findViewById(R.id.btn_next_question);
        btnNextQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_FIRST_USER);
                finish();
            }
        });
    }

    private void onShowLineNumbersToggled(final boolean enableLineNumbers) {
        codeView.setShowLineNumbers(enableLineNumbers);
        codeView.refresh();
    }

    private void onZoomSupportToggled(final boolean enableZooming) {
        codeView.setZoomSupportEnabled(enableZooming);
        codeView.refresh();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
