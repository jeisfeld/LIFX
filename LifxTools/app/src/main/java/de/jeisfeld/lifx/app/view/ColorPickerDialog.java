/*
 * Original file designed and developed by 2017 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.jeisfeld.lifx.app.view;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.skydoves.colorpickerview.listeners.ColorListener;
import com.skydoves.colorpickerview.listeners.ColorPickerViewListener;
import com.skydoves.colorpickerview.preference.ColorPickerPreferenceManager;
import com.skydoves.colorpickerview.sliders.AlphaSlideBar;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

import androidx.appcompat.app.AlertDialog;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.home.DeviceAdapter;
import de.jeisfeld.lifx.app.home.LightViewModel;
import de.jeisfeld.lifx.app.util.ColorUtil;
import de.jeisfeld.lifx.lan.type.Color;
import de.jeisfeld.lifx.lan.util.TypeUtil;

/**
 * Variation of original com.skydoves.colorpickerview.ColorPickerDialog.
 */
public class ColorPickerDialog extends AlertDialog {

	/**
	 * Constructor.
	 *
	 * @param context The context.
	 */
	public ColorPickerDialog(final Context context) {
		super(context);
	}

	/**
	 * Update a color picker view with color from a light.
	 *
	 * @param colorPickerView The color picker view to be updated.
	 * @param color           the color from which to update.
	 */
	public static void updateColorPickerFromLight(final ColorPickerView colorPickerView, final Color color) {
		if (color != null) {
			int radius = colorPickerView.getMeasuredWidth() / 2;
			int heightDifference = (colorPickerView.getMeasuredHeight() - colorPickerView.getMeasuredWidth()) / 2;
			double hue01 = TypeUtil.toDouble(color.getHue());
			double saturation01 =
					color.getSaturation() == 0 ? 0 : 0.1 + 0.9 * TypeUtil.toDouble(color.getSaturation()); // MAGIC_NUMBER

			double x = saturation01 * Math.cos(2 * Math.PI * hue01);
			double y = saturation01 * Math.sin(2 * Math.PI * hue01);

			int pointX;
			int pointY;
			if (heightDifference >= 0) {
				pointX = (int) ((x + 1) * radius);
				pointY = (int) ((1 - y) * radius) + heightDifference;
			}
			else {
				pointX = (int) ((x + 1) * radius) - heightDifference;
				pointY = (int) ((1 - y) * radius);
			}
			colorPickerView.moveSelectorPoint(pointX, pointY, ColorUtil.getAndroidColor(color));

			BrightnessSlideBar brightnessSlideBar = colorPickerView.getBrightnessSlider();
			double realWidth = (brightnessSlideBar.getMeasuredWidth() - brightnessSlideBar.getMeasuredHeight())
					/ (double) brightnessSlideBar.getMeasuredWidth();
			brightnessSlideBar.setSelectorPosition(
					(float) (TypeUtil.toDouble(color.getBrightness()) * realWidth + (1 - realWidth) / 2));

			AlphaSlideBar alphaSlideBar = colorPickerView.getAlphaSlideBar();
			if (alphaSlideBar != null) {
				double relativeColorTemp = DeviceAdapter.colorTemperatureToProgress(color.getColorTemperature()) / 120.0; // MAGIC_NUMBER
				alphaSlideBar.setSelectorPosition((float) (relativeColorTemp * realWidth + (1 - realWidth) / 2));
			}
		}
	}

	/**
	 * Update a brightness/colorTemp view with color from a light.
	 *
	 * @param colorPickerView The brightness/colorTemp view to be updated.
	 * @param color           The color for update.
	 */
	public static void updateBrightnessColorTempFromLight(final ColorPickerView colorPickerView, final Color color) {
		if (color != null) {
			double x = colorPickerView.getMeasuredWidth() * TypeUtil.toDouble(color.getBrightness());
			double y = colorPickerView.getMeasuredHeight()
					* (1 - DeviceAdapter.colorTemperatureToProgress(color.getColorTemperature()) / 120.0); // MAGIC_NUMBER
			colorPickerView.moveSelectorPoint((int) x, (int) y, ColorUtil.getAndroidColor(color));
		}
	}

	/**
	 * Prepare the color picker.
	 *
	 * @param parentView      The parent view.
	 * @param colorPickerView The view of the color picker.
	 */
	public static void prepareColorPickerView(final View parentView, final ColorPickerView colorPickerView) {
		ColorTemperatureSlideBar colorTemperatureSlider = parentView.findViewById(R.id.ColorTemperatureSlideBar);
		if (colorTemperatureSlider != null) {
			colorPickerView.attachAlphaSlider(colorTemperatureSlider);
		}
		BrightnessSlideBar brightnessSlider = parentView.findViewById(R.id.BrightnessSlideBar);
		if (brightnessSlider != null) {
			colorPickerView.attachBrightnessSlider(brightnessSlider);
		}
		colorPickerView.setColorListener(
				(ColorEnvelopeListener) (envelope, fromUser) -> {
					// nothing
				});
	}

	/**
	 * Builder class for create {@link ColorPickerDialog}.
	 */
	public static class Builder extends AlertDialog.Builder {
		/**
		 * The colorPickerView contained in the dialog.
		 */
		private final ColorPickerView mColorPickerView;
		/**
		 * The parent view.
		 */
		private final View mParentView;

		/**
		 * Constructor.
		 *
		 * @param context          The context
		 * @param layoutResourceId The layout resource
		 */
		public Builder(final Context context, final int layoutResourceId) {
			super(context);
			mParentView = View.inflate(getContext(), layoutResourceId, null);
			mColorPickerView = mParentView.findViewById(R.id.ColorPickerView);
			prepareColorPickerView(mParentView, mColorPickerView);
			super.setView(mParentView);
		}

		/**
		 * Prepare the view to initialize with data from a light.
		 *
		 * @param model The light view model.
		 * @return {@link Builder}.
		 */
		public Builder initializeFromLight(final LightViewModel model) {
			mColorPickerView.getViewTreeObserver()
					.addOnGlobalLayoutListener(() -> updateColorPickerFromLight(mColorPickerView, model.getColor().getValue()));
			return this;
		}

		/**
		 * Prepare the view to initialize with data from a light as brightness and color temperature.
		 *
		 * @param model The light view model.
		 * @return {@link Builder}.
		 */
		public Builder initializeFromBrightnessColorTemp(final LightViewModel model) {
			mColorPickerView.getViewTreeObserver()
					.addOnGlobalLayoutListener(() -> updateBrightnessColorTempFromLight(mColorPickerView, model.getColor().getValue()));
			return this;
		}

		/**
		 * gets {@link ColorPickerView} on {@link Builder}.
		 *
		 * @return {@link ColorPickerView}.
		 */
		public ColorPickerView getColorPickerView() {
			return mColorPickerView;
		}

		/**
		 * Get the parent view.
		 *
		 * @return The parent view.
		 */
		public View getParentView() {
			return mParentView;
		}

		/**
		 * sets positive button with {@link ColorPickerViewListener} on the {@link ColorPickerDialog}.
		 *
		 * @param textId        string resource integer id.
		 * @param colorListener {@link ColorListener}.
		 * @return {@link Builder}.
		 */
		public Builder setPositiveButton(final int textId, final ColorPickerViewListener colorListener) {
			super.setPositiveButton(textId, getOnClickListener(colorListener));
			return this;
		}

		/**
		 * sets positive button with {@link ColorPickerViewListener} on the {@link ColorPickerDialog}.
		 *
		 * @param text          string text value.
		 * @param colorListener {@link ColorListener}.
		 * @return {@link Builder}.
		 */
		public Builder setPositiveButton(
				final CharSequence text, final ColorPickerViewListener colorListener) {
			super.setPositiveButton(text, getOnClickListener(colorListener));
			return this;
		}

		/**
		 * Set the color listerer.
		 *
		 * @param colorListener The color listener.
		 * @return {@link Builder}.
		 */
		public Builder setColorListener(final ColorListener colorListener) {
			mColorPickerView.setColorListener(colorListener);
			return this;
		}

		@Override
		public final Builder setNegativeButton(final int textId, final OnClickListener listener) {
			super.setNegativeButton(textId, listener);
			return this;
		}

		@Override
		public final Builder setNegativeButton(final CharSequence text, final OnClickListener listener) {
			super.setNegativeButton(text, listener);
			return this;
		}

		/**
		 * Get the onClickListener.
		 *
		 * @param colorListener The color listener.
		 * @return The onClickListener.
		 */
		private OnClickListener getOnClickListener(final ColorPickerViewListener colorListener) {
			return (dialogInterface, i) -> {
				if (colorListener instanceof ColorListener) {
					((ColorListener) colorListener).onColorSelected(mColorPickerView.getColor(), true);
				}
				else if (colorListener instanceof ColorEnvelopeListener) {
					((ColorEnvelopeListener) colorListener)
							.onColorSelected(mColorPickerView.getColorEnvelope(), true);
				}
				if (getColorPickerView() != null) {
					ColorPickerPreferenceManager.getInstance(getContext())
							.saveColorPickerData(getColorPickerView());
				}
			};
		}

		/**
		 * shows a created {@link ColorPickerDialog}.
		 *
		 * @return {@link AlertDialog}.
		 */
		@Override
		public AlertDialog show() {
			super.setView(mParentView);
			return super.show();
		}

		@Override
		public final Builder setTitle(final int titleId) {
			super.setTitle(titleId);
			return this;
		}

		@Override
		public final Builder setTitle(final CharSequence title) {
			super.setTitle(title);
			return this;
		}

		@Override
		public final Builder setCustomTitle(final View customTitleView) {
			super.setCustomTitle(customTitleView);
			return this;
		}

		@Override
		public final Builder setMessage(final int messageId) {
			super.setMessage(getContext().getString(messageId));
			return this;
		}

		@Override
		public final Builder setMessage(final CharSequence message) {
			super.setMessage(message);
			return this;
		}

		@Override
		public final Builder setIcon(final int iconId) {
			super.setIcon(iconId);
			return this;
		}

		@Override
		public final Builder setIcon(final Drawable icon) {
			super.setIcon(icon);
			return this;
		}

		@Override
		public final Builder setIconAttribute(final int attrId) {
			super.setIconAttribute(attrId);
			return this;
		}

		@Override
		public final Builder setCancelable(final boolean cancelable) {
			super.setCancelable(cancelable);
			return this;
		}

		@Override
		public final Builder setOnCancelListener(final OnCancelListener onCancelListener) {
			super.setOnCancelListener(onCancelListener);
			return this;
		}

		@Override
		public final Builder setOnDismissListener(final OnDismissListener onDismissListener) {
			super.setOnDismissListener(onDismissListener);
			return this;
		}

		@Override
		public final Builder setOnKeyListener(final OnKeyListener onKeyListener) {
			super.setOnKeyListener(onKeyListener);
			return this;
		}

		@Override
		public final Builder setPositiveButton(final int textId, final OnClickListener listener) {
			super.setPositiveButton(textId, listener);
			return this;
		}

		@Override
		public final Builder setPositiveButton(final CharSequence text, final OnClickListener listener) {
			super.setPositiveButton(text, listener);
			return this;
		}

		@Override
		public final Builder setNeutralButton(final int textId, final OnClickListener listener) {
			super.setNeutralButton(textId, listener);
			return this;
		}

		@Override
		public final Builder setNeutralButton(final CharSequence text, final OnClickListener listener) {
			super.setNeutralButton(text, listener);
			return this;
		}

		@Override
		public final Builder setItems(final int itemsId, final OnClickListener listener) {
			super.setItems(itemsId, listener);
			return this;
		}

		@Override
		public final Builder setItems(final CharSequence[] items, final OnClickListener listener) {
			super.setItems(items, listener);
			return this;
		}

		@Override
		public final Builder setAdapter(final ListAdapter adapter, final OnClickListener listener) {
			super.setAdapter(adapter, listener);
			return this;
		}

		@Override
		public final Builder setCursor(final Cursor cursor, final OnClickListener listener, final String labelColumn) {
			super.setCursor(cursor, listener, labelColumn);
			return this;
		}

		@Override
		public final Builder setMultiChoiceItems(
				final int itemsId, final boolean[] checkedItems, final OnMultiChoiceClickListener listener) {
			super.setMultiChoiceItems(itemsId, checkedItems, listener);
			return this;
		}

		@Override
		public final Builder setMultiChoiceItems(
				final CharSequence[] items, final boolean[] checkedItems, final OnMultiChoiceClickListener listener) {
			super.setMultiChoiceItems(items, checkedItems, listener);
			return this;
		}

		@Override
		public final Builder setMultiChoiceItems(
				final Cursor cursor,
				final String isCheckedColumn,
				final String labelColumn,
				final OnMultiChoiceClickListener listener) {
			super.setMultiChoiceItems(cursor, isCheckedColumn, labelColumn, listener);
			return this;
		}

		@Override
		public final Builder setSingleChoiceItems(final int itemsId, final int checkedItem, final OnClickListener listener) {
			super.setSingleChoiceItems(itemsId, checkedItem, listener);
			return this;
		}

		@Override
		public final Builder setSingleChoiceItems(
				final Cursor cursor, final int checkedItem, final String labelColumn, final OnClickListener listener) {
			super.setSingleChoiceItems(cursor, checkedItem, labelColumn, listener);
			return this;
		}

		@Override
		public final Builder setSingleChoiceItems(
				final CharSequence[] items, final int checkedItem, final OnClickListener listener) {
			super.setSingleChoiceItems(items, checkedItem, listener);
			return this;
		}

		@Override
		public final Builder setSingleChoiceItems(
				final ListAdapter adapter, final int checkedItem, final OnClickListener listener) {
			super.setSingleChoiceItems(adapter, checkedItem, listener);
			return this;
		}

		@Override
		public final Builder setOnItemSelectedListener(final AdapterView.OnItemSelectedListener listener) {
			super.setOnItemSelectedListener(listener);
			return this;
		}

		@Override
		public final Builder setView(final int layoutResId) {
			super.setView(layoutResId);
			return this;
		}

		@Override
		public final Builder setView(final View view) {
			super.setView(view);
			return this;
		}
	}
}
