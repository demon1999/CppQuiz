package com.vsklamm.cppquiz.quizMVP;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.SparseIntArray;

import com.vsklamm.cppquiz.App;
import com.vsklamm.cppquiz.database.AppDatabase;
import com.vsklamm.cppquiz.model.Question;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

import static java.lang.Math.max;

public class GameLogic implements Serializable { // TODO: rename methods

    public static final String CPP_STANDARD = "CPP_STANDARD";
    private static volatile GameLogic gameLogicInstance;
    private WeakReference<GameLogic.GameLogicCallbacks> listener;
    private LinkedHashSet<Integer> questionsIds;
    private Question currentQuestion;

    private GameLogic() {
        if (gameLogicInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static GameLogic getInstance() {
        if (gameLogicInstance == null) {
            synchronized (GameLogic.class) {
                if (gameLogicInstance == null) {
                    gameLogicInstance = new GameLogic();
                }
            }
        }
        return gameLogicInstance;
    }

    public void initNewData(@NonNull Context context, @NonNull final String cppStandard, @NonNull LinkedHashSet<Integer> questionsIds) {
        this.questionsIds = questionsIds;
        try {
            listener = new WeakReference<>((GameLogic.GameLogicCallbacks) context); // TODO: zabotat'
        } catch (ClassCastException e) {
            // ignore
        }
        setCppStandard(cppStandard);
        // Class contract says that UserData is initialized now
        LinkedHashSet<Integer> correctlyAnswered = UserData.getInstance().getCorrectlyAnsweredQuestions();
        for (Integer id : correctlyAnswered) {
            if (!questionsIds.contains(id)) {
                correctlyAnswered.remove(id);
            }
        }
        SparseIntArray attempts = UserData.getInstance().getAttempts();
        for (int it = 0; it < attempts.size(); ++it) {
            if (!questionsIds.contains(attempts.keyAt(it))) {
                attempts.removeAt(it);
            }
        }
        updateGameState();
    }

    public void randomQuestion() {
        int randomId = getUnansweredQuestion();
        if (randomId == -1) {
            listener.get().noMoreQuestions();
        } else {
            questionById(randomId);
        }
    }

    public void questionById(final int questionId) {
        AppDatabase db = App.getInstance().getDatabase();

        db.questionDao().findById(questionId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<Question>() {
                    @Override
                    public void onSuccess(Question question) {
                        currentQuestion = question;
                        updateGameState();
                        listener.get().onQuestionLoaded(
                                currentQuestion,
                                max(3 - UserData.getInstance().attemptsGivenFor(currentQuestion.getId()), 0)
                        );
                    }

                    @Override
                    public void onError(Throwable e) {
                        // ignore
                    }
                });
    }

    public void questionHint() {
        String hint = currentQuestion.getHint();
        listener.get().onHintReceived(hint);
    }

    public void checkAnswer() {
        int currentId = currentQuestion.getId();
        UserData.getInstance().registerAttempt(currentId);

        boolean correct = currentQuestion.compareWithAnswer(UserData.getInstance().givenAnswer);
        if (correct) {
            UserData.getInstance().registerCorrectAnswer(currentId);
            UserData.getInstance().saveUserData();
            updateGameState();
            listener.get().onCorrectAnswered(currentQuestion, max(3 - UserData.getInstance().attemptsGivenFor(currentId), 0));
        } else {
            UserData.getInstance().saveUserData();
            listener.get().onIncorrectAnswered(max(3 - UserData.getInstance().attemptsGivenFor(currentId), 0));
        }
    }

    public void giveUp() {
        int attemptsGivenFor = UserData.getInstance().attemptsGivenFor(currentQuestion.getId());
        if (attemptsGivenFor >= 3) {
            listener.get().onGiveUp(currentQuestion);
        } else {
            listener.get().tooEarlyToGiveUp(3 - attemptsGivenFor);
        }
    }

    private int getUnansweredQuestion() { // TODO: too long & create exception
        try {
            LinkedHashSet<Integer> availableQuestions = new LinkedHashSet<>(questionsIds);
            LinkedHashSet<Integer> correctlyAnswered = UserData.getInstance().getCorrectlyAnsweredQuestions();
            availableQuestions.removeAll(correctlyAnswered);

            if (availableQuestions.size() == 0) {
                return -1;
            } else {
                List<Integer> availableAsList = new ArrayList<>(availableQuestions);
                return availableAsList.get(new Random().nextInt(availableAsList.size()));
            }
        } catch (NullPointerException e) { // TODO: handle this
            return 1;
        }
    }

    public Question getCurrentQuestion() {
        return currentQuestion;
    }

    private void setCppStandard(final String cppStandard) {
        listener.get().onCppStandardChanged(cppStandard);
    }

    private void updateGameState() { // TODO: kludge ?
        if (currentQuestion != null && questionsIds != null) {
            listener.get().onGameStateChanged(
                    currentQuestion.getId(),
                    UserData.getInstance().getCorrectlyAnsweredQuestions().size(),
                    questionsIds.size()
            );
        }
    }

    @SuppressWarnings("unused")
    protected GameLogic readResolve() {
        return getInstance();
    }

    public interface GameLogicCallbacks {

        void onCppStandardChanged(@NonNull final String cppStandard); // same

        void onGameStateChanged(final int questionId, final int correct, final int all); // diff

        void onQuestionLoaded(@NonNull final Question question, int attemptsRequired); // diff or with extra method (progress)

        void onHintReceived(@NonNull final String hint); // diff or with extra method (score)

        void onCorrectAnswered(@NonNull final Question question, final int attemptsRequired); // diff or with extra method (progress)

        void onIncorrectAnswered(final int attemptsRequired); // diff or with extra method (progress)

        void onGiveUp(@NonNull final Question question); // none

        void tooEarlyToGiveUp(final int attemptsRequired); // none

        void noMoreQuestions(); // onFinishQuiz instead

        void onErrorHappens();
    }
}
