package de.jeisfeld.lifx.app.util;

import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;

/**
 * Utility class for operations with images.
 */
public final class ImageUtil {
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
		@SuppressWarnings("deprecation")
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
}
