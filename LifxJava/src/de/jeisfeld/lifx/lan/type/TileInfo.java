package de.jeisfeld.lifx.lan.type;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Information for a single tile.
 */
public class TileInfo implements Serializable {
	/**
	 * The default serializable version id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The x gravity.
	 */
	private short mAccelerationX;
	/**
	 * The y gravity.
	 */
	private short mAccelerationY;
	/**
	 * The z gravity.
	 */
	private short mAccelerationZ;
	/**
	 * The rotation.
	 */
	private Rotation mRotation;
	/**
	 * The x position of the tile.
	 */
	private float mUserX;
	/**
	 * The y position of the tile.
	 */
	private float mUserY;
	/**
	 * The width of the tile.
	 */
	private byte mWidth;
	/**
	 * The height of the tile.
	 */
	private byte mHeight;
	/**
	 * The vendor.
	 */
	private Vendor mVendor;
	/**
	 * The product.
	 */
	private Product mProduct;
	/**
	 * The version.
	 */
	private int mVersion;
	/**
	 * The build time.
	 */
	private Date mBuildTime;
	/**
	 * The minor version.
	 */
	private short mMinorVersion;
	/**
	 * The major version.
	 */
	private short mMajorVersion;
	/**
	 * The min x coordinate.
	 */
	private int mMinX;
	/**
	 * The min y coordinate.
	 */
	private int mMinY;

	/**
	 * Extract tile data from a StateDeviceChain byte buffer.
	 *
	 * @param byteBuffer the byte buffer.
	 * @return The tile data.
	 */
	public static TileInfo readFromByteBuffer(final ByteBuffer byteBuffer) {
		TileInfo tileInfo = new TileInfo();
		tileInfo.mAccelerationX = byteBuffer.getShort();
		tileInfo.mAccelerationY = byteBuffer.getShort();
		tileInfo.mAccelerationZ = byteBuffer.getShort();
		tileInfo.mRotation = calculateRotation(tileInfo.mAccelerationX, tileInfo.mAccelerationY, tileInfo.mAccelerationZ);
		byteBuffer.getShort();
		tileInfo.mUserX = byteBuffer.getFloat();
		tileInfo.mUserY = byteBuffer.getFloat();
		tileInfo.mWidth = byteBuffer.get();
		tileInfo.mHeight = byteBuffer.get();
		byteBuffer.get();
		tileInfo.mVendor = Vendor.fromInt(byteBuffer.getInt());
		tileInfo.mProduct = Product.fromId(byteBuffer.getInt());
		tileInfo.mVersion = byteBuffer.getInt();
		tileInfo.mBuildTime = new Date(byteBuffer.getLong() / 1000000); // MAGIC_NUMBER
		byteBuffer.getLong(); // reserved
		tileInfo.mMinorVersion = byteBuffer.getShort();
		tileInfo.mMajorVersion = byteBuffer.getShort();
		byteBuffer.getInt();
		return tileInfo;
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Product=").append(mProduct.getName()).append(" (v").append(mVersion).append("), ");
		sb.append("Firmware=").append(mMajorVersion).append(".").append(mMinorVersion).append(", ");
		sb.append("Size=(").append(mWidth).append(",").append(mHeight).append("), ");
		sb.append("Position=(").append(mUserX).append(",").append(mUserY).append("),(").append(mMinX).append(",").append(mMinY).append("), ");
		sb.append("Rotation=").append(getRotation()).append("(");
		sb.append(mAccelerationX).append(",").append(mAccelerationY).append(",").append(mAccelerationZ).append(")");
		return sb.toString();
	}

	/**
	 * Determine the min coordinates.
	 *
	 * @param xOffset the x offset.
	 * @param yOffset the y offset.
	 */
	public void determineMinCoordinates(final float xOffset, final float yOffset) {
		mMinX = Math.round((mUserX - xOffset) * mWidth);
		mMinY = Math.round((mUserY - yOffset) * mHeight);
	}

	/**
	 * Get the x gravity.
	 *
	 * @return the x gravity.
	 */
	public final short getAccelerationX() {
		return mAccelerationX;
	}

	/**
	 * Get the y gravity.
	 *
	 * @return the y gravity.
	 */
	public final short getAccelerationY() {
		return mAccelerationY;
	}

	/**
	 * Get the z gravity.
	 *
	 * @return the z gravity.
	 */
	public final short getAccelerationZ() {
		return mAccelerationZ;
	}

	/**
	 * Get the rotation.
	 *
	 * @return the rotation.
	 */
	public final Rotation getRotation() {
		return mRotation;
	}

	/**
	 * Get the x position of the tile.
	 *
	 * @return the x position of the tile.
	 */
	public final float getUserX() {
		return mUserX;
	}

	/**
	 * Get the y position of the tile.
	 *
	 * @return the y position of the tile.
	 */
	public final float getUserY() {
		return mUserY;
	}

	/**
	 * Get the width of the tile.
	 *
	 * @return the width of the tile.
	 */
	public final byte getWidth() {
		return mWidth;
	}

	/**
	 * Get the height of the tile.
	 *
	 * @return the height of the tile.
	 */
	public final byte getHeight() {
		return mHeight;
	}

	/**
	 * Get the vendor.
	 *
	 * @return the Vendor
	 */
	public final Vendor getVendor() {
		return mVendor;
	}

	/**
	 * Get the product.
	 *
	 * @return the Product
	 */
	public final Product getProduct() {
		return mProduct;
	}

	/**
	 * Get the version.
	 *
	 * @return the Version
	 */
	public final int getVersion() {
		return mVersion;
	}

	/**
	 * Get the build time.
	 *
	 * @return the build time
	 */
	public final Date getmBuildTime() {
		return mBuildTime;
	}

	/**
	 * Get the minor firmware version.
	 *
	 * @return the minor firmware version
	 */
	public final short getMinorVersion() {
		return mMinorVersion;
	}

	/**
	 * Get the major firmware version.
	 *
	 * @return the major firmware version.
	 */
	public final short getmMajorVersion() {
		return mMajorVersion;
	}

	/**
	 * Get the min x coordinate.
	 *
	 * @return the min x coordinate.
	 */
	public final int getMinX() {
		return mMinX;
	}

	/**
	 * Get the min y coordinate.
	 *
	 * @return the min y coordinate.
	 */
	public final int getMinY() {
		return mMinY;
	}

	/**
	 * Get the rotation status from the acceleration values.
	 *
	 * @param accelerationX The x acceleration
	 * @param accelerationY The y acceleration
	 * @param accelerationZ The z acceleration
	 * @return The rotation status.
	 */
	public static Rotation calculateRotation(final float accelerationX, final float accelerationY, final float accelerationZ) {
		float absX = Math.abs(accelerationX);
		float absY = Math.abs(accelerationY);
		float absZ = Math.abs(accelerationZ);

		if (accelerationX == -1 && accelerationY == -1 && accelerationZ == -1) {
			// Invalid data, assume right-side up.
			return Rotation.UPRIGHT;

		}
		else if (absX > absY && absX > absZ) {
			if (accelerationX > 0) {
				return Rotation.ROTATE_RIGHT;
			}
			else {
				return Rotation.ROTATE_LEFT;
			}

		}
		else if (absZ > absX && absZ > absY) {
			if (accelerationZ > 0) {
				return Rotation.FACE_UP;
			}
			else {
				return Rotation.FACE_DOWN;
			}

		}
		else {
			if (accelerationY > 0) {
				return Rotation.UPSIDE_DOWN;
			}
			else {
				return Rotation.UPRIGHT;
			}
		}
	}

	/**
	 * Enum for rotation position of the tile.
	 */
	public enum Rotation {
		/**
		 * Upright position.
		 */
		UPRIGHT,
		/**
		 * Right rotated position.
		 */
		ROTATE_RIGHT,
		/**
		 * Upside down position.
		 */
		UPSIDE_DOWN,
		/**
		 * Rotate left position.
		 */
		ROTATE_LEFT,
		/**
		 * Face up position.
		 */
		FACE_UP,
		/**
		 * Face down position.
		 */
		FACE_DOWN
	}

}
