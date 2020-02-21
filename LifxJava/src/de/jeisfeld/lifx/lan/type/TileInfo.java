package de.jeisfeld.lifx.lan.type;

import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Information for a single tile.
 */
public class TileInfo {
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
		sb.append("Position=(").append(mUserX).append(",").append(mUserY).append("), ");
		sb.append("Gravity=").append(getRotationString()).append("(");
		sb.append(mAccelerationX).append(",").append(mAccelerationY).append(",").append(mAccelerationZ).append(")");
		return sb.toString();
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
	 * Get a string describing the rotation status.
	 *
	 * @return The rotation status.
	 */
	public String getRotationString() {
		float absX = Math.abs(mAccelerationX);
		float absY = Math.abs(mAccelerationY);
		float absZ = Math.abs(mAccelerationZ);

		if (mAccelerationX == -1 && mAccelerationY == -1 && mAccelerationZ == -1) {
			// Invalid data, assume right-side up.
			return "normal";

		}
		else if (absX > absY && absX > absZ) {
			if (mAccelerationX > 0) {
				return "rotateRight";
			}
			else {
				return "rotateLeft";
			}

		}
		else if (absZ > absX && absZ > absY) {
			if (mAccelerationZ > 0) {
				return "faceUp";
			}
			else {
				return "faceDown";
			}

		}
		else {
			if (mAccelerationY > 0) {
				return "upsideDown";
			}
			else {
				return "normal";
			}
		}
	}
}
