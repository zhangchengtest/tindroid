package co.tinode.tindroid;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.appcompat.app.AppCompatActivity;
import co.tinode.tindroid.db.BaseDb;

/**
 * Splash screen on startup
 */
public class SplashActivity extends AppCompatActivity {

    private Timer_Countdown timer_Countdown = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timer_Countdown = new Timer_Countdown(5000, 1000);
        timer_Countdown.start();

    }


    class Timer_Countdown extends CountDownTimer
    {
        public Timer_Countdown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            timer_Countdown.cancel();
            // No need to check for live connection here.

            // Send user to appropriate screen:
            // 1. If we have an account and no credential validation is needed, send to ChatsActivity.
            // 2. If we don't have an account or credential validation is required send to LoginActivity.
            Intent launch = new Intent(SplashActivity.this, BaseDb.getInstance().isReady() ?
                    ChatsActivity.class : LoginActivity.class);
            launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launch);
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }
    }
}
