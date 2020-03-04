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

package de.jeisfeld.lifx.app.ui.view;

import com.skydoves.colorpickerview.sliders.AlphaSlideBar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;

/**
 * ColorTemperatureSlideBar extends AlphaSlideBar to support color temperature instead of alpha.
 */
public class ColorTemperatureSlideBar extends AlphaSlideBar {
	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @see android.view.View#View(Context, AttributeSet, int)
	 */
	public ColorTemperatureSlideBar(final Context context) {
		super(context);
	}

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 * @see android.view.View#View(Context, AttributeSet, int)
	 */
	public ColorTemperatureSlideBar(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context      The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @param attrs        The attributes of the XML tag that is inflating the view.
	 * @param defStyleAttr An attribute in the current theme that contains a reference to a style resource that supplies default
	 *                     values for the view. Can be 0 to not look for defaults.
	 * @see android.view.View#View(Context, AttributeSet, int)
	 */
	public ColorTemperatureSlideBar(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public final void updatePaint(final Paint colorPaint) {
		Shader shader = new LinearGradient(
				0, 0, getWidth(), getHeight(),
				new int[]{Color.rgb(255, 96, 0), Color.YELLOW, Color.WHITE, Color.rgb(128, 255, 255)}, // MAGIC_NUMBER
				new float[]{0, (float) 0.3, (float) 0.6, 1}, // MAGIC_NUMBER
				Shader.TileMode.CLAMP);
		colorPaint.setShader(shader);
	}

	@Override
	public final int assembleColor() {
		return getColor();
	}
}
