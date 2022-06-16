package mx.mobile.solution.nabia04.main.util;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import mx.mobile.solution.nabia04.R;

public class NewClass {
    private final Handler imageSwitcherHandler = new Handler(Looper.getMainLooper());

    private void showRunnable(Context context){
        final ImageView pic_view = new ImageView(context);
        final int animationCounter = 1;
        RunnableTask task = new RunnableTask();
        imageSwitcherHandler.post(task);
    }


    private class RunnableTask implements Runnable {
        int animationCounter = 1;
        @Override
        public void run() {
            switch (animationCounter++) {
                case 1:

                    break;
                case 2:

                    break;
                case 3:
                    break;
            }
            animationCounter %= 4;
            if(animationCounter == 0 ) animationCounter = 1;
            imageSwitcherHandler.postDelayed(this, 3000);
        }
    }
}
