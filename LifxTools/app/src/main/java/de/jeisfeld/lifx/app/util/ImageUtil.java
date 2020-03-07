package de.jeisfeld.lifx.app.util;

import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import androidx.annotation.NonNull;

/**
 * Utility class for operations with images.
 */
public final class ImageUtil {
	/**
	 * The max size of a byte.
	 */
	private static final int BYTE = 255;

	/**
	 * Hide default constructor.
	 */
	private ImageUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Return a bitmap of a photo.
	 *
	 * @param context The triggering context.
	 * @param uri The Uri of the image.
	 * @return the bitmap.
	 * @throws IOException Exception while parsing bitmap.
	 */
	public static Bitmap getBitmapFromUri(final Context context, final Uri uri) throws IOException {
		Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
		if (bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0 // BOOLEAN_EXPRESSION_COMPLEXITY
				|| bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
			throw new IOException("Failed to get bitmap from URI " + uri);
		}
		return rotateBitmap(bitmap, getOrientation(context, uri));
	}

	/**
	 * Get the orientation of an image from its URI.
	 *
	 * @param context The context.
	 * @param photoUri The URI.
	 * @return The orientation.
	 */
	public static int getOrientation(final Context context, final Uri photoUri) {
		Cursor cursor = context.getContentResolver().query(photoUri,
				new String[] {MediaColumns.ORIENTATION}, null, null, null);
		if (cursor == null) {
			return 0;
		}
		if (cursor.getCount() != 1) {
			cursor.close();
			return 0;
		}
		cursor.moveToFirst();
		int result = cursor.getInt(0);
		cursor.close();
		return result;
	}

	/**
	 * Rotate a bitmap.
	 *
	 * @param source The original bitmap
	 * @param angle The rotation angle
	 * @return the rotated bitmap.
	 */
	private static Bitmap rotateBitmap(final Bitmap source, final float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

	/**
	 * Update contrast and brightness of a bitmap.
	 *
	 * @param bmp input bitmap
	 * @param contrast 0..infinity - 1 is default
	 * @param brightness -1..1 - 0 is default
	 * @param saturation 1/3..infinity - 1 is default
	 * @return new bitmap
	 */
	public static Bitmap changeBitmapColors(@NonNull final Bitmap bmp, final float contrast, final float brightness, final float saturation) {
		if (contrast == 1 && brightness == 0 && saturation == 1) {
			return bmp;
		}

		// some baseCalculations for the mapping matrix
		float offset = BYTE / 2f * (1 - contrast + brightness * contrast + brightness);
		float oppositeSaturation = (1 - saturation) / 2;

		ColorMatrix cm = new ColorMatrix(new float[] { //
				contrast * saturation, contrast * oppositeSaturation, contrast * oppositeSaturation, 0, offset, //
				contrast * oppositeSaturation, contrast * saturation, contrast * oppositeSaturation, 0, offset, //
				contrast * oppositeSaturation, contrast * oppositeSaturation, contrast * saturation, 0, offset, //
				0, 0, 0, 1, 0});

		Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

		Canvas canvas = new Canvas(ret);

		Paint paint = new Paint();
		paint.setColorFilter(new ColorMatrixColorFilter(cm));
		canvas.drawBitmap(bmp, 0, 0, paint);

		return ret;
	}

	/**
	 * Create a scaled bitmap from bitmap.
	 *
	 * @param bitmap The original bitmap.
	 * @param width The new width.
	 * @param height The new height.
	 * @param filter true if filtering should be applied.
	 * @return The rescaled bitmap.
	 */
	public static Bitmap createScaledBitmap(final Bitmap bitmap, final int width, final int height, final boolean filter) {
		// Antialias works better when first scaling to double the size.
		if (filter) {
			return Bitmap.createScaledBitmap(Bitmap.createScaledBitmap(bitmap, 2 * width, 2 * height, true), width, height, true);
		}
		else {
			return Bitmap.createScaledBitmap(bitmap, width, height, false);
		}
	}
}
