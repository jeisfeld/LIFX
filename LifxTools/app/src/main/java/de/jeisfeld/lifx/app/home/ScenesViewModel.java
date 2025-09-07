package de.jeisfeld.lifx.app.home;

import android.content.Context;

import androidx.lifecycle.LiveData;

import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.type.Power;

/**
 * View model for scenes entry on home screen.
 */
public class ScenesViewModel extends MainViewModel {
        /**
         * Constructor.
         *
         * @param context the context
         */
        public ScenesViewModel(final Context context) {
                super(context);
        }

        @Override
        public CharSequence getLabel() {
                Context ctx = getContext().get();
                return ctx == null ? "" : ctx.getString(R.string.menu_scenes);
        }

        @Override
        public void checkPower() {
                // no device
        }

        @Override
        public void togglePower() {
                // no device
        }

        @Override
        public LiveData<Color> getColor() {
                return null;
        }

        @Override
        public void updatePowerButton(final Power power) {
                // ignore
        }
}
