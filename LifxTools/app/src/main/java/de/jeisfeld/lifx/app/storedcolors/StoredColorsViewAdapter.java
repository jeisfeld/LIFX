package de.jeisfeld.lifx.app.storedcolors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import de.jeisfeld.lifx.app.Application;
import de.jeisfeld.lifx.app.R;
import de.jeisfeld.lifx.app.alarms.AlarmRegistry;
import de.jeisfeld.lifx.app.home.MultizoneViewModel.FlaggedMultizoneColors;
import de.jeisfeld.lifx.app.managedevices.DeviceRegistry;
import de.jeisfeld.lifx.app.util.ColorUtil;
import de.jeisfeld.lifx.app.util.DialogUtil;
import de.jeisfeld.lifx.app.util.DialogUtil.RequestInputDialogFragment.RequestInputDialogListener;
import de.jeisfeld.lifx.app.util.PreferenceUtil;
import de.jeisfeld.lifx.lan.TileChain;
import de.jeisfeld.lifx.lan.type.MultizoneColors;
import de.jeisfeld.lifx.lan.type.TileChainColors;

/**
 * Adapter for the RecyclerView that allows to sort devices.
 */
public class StoredColorsViewAdapter extends RecyclerView.Adapter<StoredColorsViewAdapter.MyViewHolder>
		implements StoredColorsItemMoveCallback.ItemTouchHelperContract {
	/**
	 * The list of stored colors as view data.
	 */
	private final List<StoredColor> mStoredColors;
	/**
	 * The list of device ids.
	 */
	private final List<Integer> mColorIds;
	/**
	 * The listener identifying start of drag.
	 */
	private StartDragListener mStartDragListener;
	/**
	 * A reference to the fragment.
	 */
	private final WeakReference<Fragment> mFragment;

	/**
	 * Constructor.
	 *
	 * @param fragment     the calling fragment.
	 * @param recyclerView The recycler view.
	 * @param deviceId     The device id.
	 */
	public StoredColorsViewAdapter(final Fragment fragment, final RecyclerView recyclerView, final Integer deviceId) {
		if (deviceId == null) {
			mStoredColors = ColorRegistry.getInstance().getStoredColors();
		}
		else {
			mStoredColors = ColorRegistry.getInstance().getStoredColors(deviceId);
		}
		mColorIds = PreferenceUtil.getSharedPreferenceIntList(R.string.key_color_ids);
		mFragment = new WeakReference<>(fragment);
	}

	/**
	 * Set the listener identifying start of drag.
	 *
	 * @param startDragListener The listener.
	 */
	public void setStartDragListener(final StartDragListener startDragListener) {
		mStartDragListener = startDragListener;
	}

	@Override
	public final MyViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
		View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_view_stored_colors, parent, false);
		return new MyViewHolder(itemView);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public final void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {
		final StoredColor storedColor = mStoredColors.get(position);
		holder.mTitle.setText(storedColor.getName());

		holder.mTitle.setOnClickListener(v -> {
			FragmentActivity activity = mFragment.get() == null ? null : mFragment.get().getActivity();
			if (activity != null) {
				DialogUtil.displayInputDialog(activity, new RequestInputDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog, final String text) {
								if (text == null || text.trim().isEmpty()) {
									DialogUtil.displayConfirmationMessage(activity,
											R.string.title_did_not_save_empty_name, R.string.message_did_not_save_empty_name);
								}
								else {
									StoredColor newColor;
									if (storedColor instanceof StoredMultizoneColors) {
										newColor = new StoredMultizoneColors(storedColor.getId(), ((StoredMultizoneColors) storedColor).getColors(),
												storedColor.getDeviceId(), text.trim());
									}
									else if (storedColor instanceof StoredTileColors) {
										newColor = new StoredTileColors(storedColor.getId(), ((StoredTileColors) storedColor).getColors(),
												storedColor.getDeviceId(), text.trim());
									}
									else {
										newColor = new StoredColor(storedColor.getId(), storedColor.getColor(),
												storedColor.getDeviceId(), text.trim());
									}

									ColorRegistry.getInstance().addOrUpdate(newColor);
									mStoredColors.set(position, newColor);
									holder.mTitle.setText(text.trim());
								}
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {
								// do nothing
							}
						}, R.string.title_dialog_change_color_name, R.string.button_rename,
						holder.mTitle.getText().toString(), R.string.message_dialog_new_color_name);
			}
		});

		if (storedColor.getLight() != null) {
			holder.mDeviceName.setText(storedColor.getLight().getLabel());
		}
		else if (storedColor.getGroup() != null) {
			holder.mDeviceName.setText(storedColor.getGroup().getGroupLabel());
		}

		holder.mDragHandle.setOnTouchListener((view, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mStartDragListener.requestDrag(holder);
			}
			else if (event.getAction() == MotionEvent.ACTION_UP) {
				view.performClick();
			}
			return false;
		});

		if (AlarmRegistry.getInstance().isUsed(storedColor)) {
			holder.mDeleteButton.setVisibility(View.INVISIBLE);
		}
		else {
			holder.mDeleteButton.setVisibility(View.VISIBLE);
			holder.mDeleteButton.setOnClickListener(v -> {
				Fragment fragment = mFragment.get();
				if (fragment != null && fragment.getActivity() != null) {
					DialogUtil.displayConfirmationMessage(fragment.getActivity(), dialog -> {
						ColorRegistry.getInstance().remove(storedColor);
						mStoredColors.remove(position);
						mColorIds.remove(position);
						notifyItemRemoved(position);
						notifyItemRangeChanged(position, mStoredColors.size() - position);
					}, null, R.string.button_cancel, R.string.button_delete, R.string.message_confirm_delete_color, storedColor.getName());
				}
			});
		}

		holder.mApplyColorButton.setOnClickListener(v -> {
			Fragment fragment = mFragment.get();
			if (fragment != null) {
				storedColor.apply(mFragment.get().getContext(), null);
			}
		});

		Fragment fragment = mFragment.get();
		Context context = fragment == null ? null : fragment.getContext();
		if (context != null) {
			holder.mApplyColorButton.setImageDrawable(getButtonDrawable(context, storedColor));
		}
	}

	/**
	 * Get the drawable to be used for display of the stored color.
	 *
	 * @param context     The context.
	 * @param storedColor The stored color.
	 * @return The drawable to be used.
	 */
	public static Drawable getButtonDrawable(final Context context, final StoredColor storedColor) {
		if (DeviceRegistry.getInstance().getRingtoneDummyLight().equals(storedColor.getLight())) {
			return ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_alarm_ringtone, context.getTheme());
		}

		GradientDrawable drawable = new GradientDrawable();
		drawable.setStroke((int) context.getResources().getDimension(R.dimen.power_button_stroke_size), android.graphics.Color.BLACK);
		drawable.setShape(GradientDrawable.OVAL);
		if (storedColor.getColor() != null) {
			drawable.setColor(ColorUtil.toAndroidDisplayColor(storedColor.getColor()));
		}

		if (storedColor instanceof StoredMultizoneColors) {
			MultizoneColors colors = ((StoredMultizoneColors) storedColor).getColors();
			if (colors instanceof FlaggedMultizoneColors) {
				colors = ((FlaggedMultizoneColors) colors).getBaseColors();
			}
			if (colors instanceof MultizoneColors.Fixed) {
				drawable.setColor(ColorUtil.toAndroidDisplayColor(((MultizoneColors.Fixed) colors).getColor()));
			}
			else {
				drawable.setShape(GradientDrawable.RECTANGLE);
				if (colors instanceof MultizoneColors.Interpolated) {
					drawable = ColorUtil.getButtonDrawable(context, ((MultizoneColors.Interpolated) colors).getColors());
				}
				else if (colors instanceof MultizoneColors.Exact) {
					drawable.setColors(ColorUtil.toAndroidDisplayColors(((MultizoneColors.Exact) colors).getColors()));
				}
				MultizoneOrientation multizoneOrientation = MultizoneOrientation.fromOrdinal(
						PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_device_multizone_orientation, storedColor.getDeviceId(), 0));
				drawable.setOrientation(multizoneOrientation.getGradientOrientation());
			}
		}

		else if (storedColor instanceof StoredTileColors) {
			TileChainColors colors = ((StoredTileColors) storedColor).getColors();
			if (colors instanceof TileChainColors.Fixed) {
				drawable.setColor(ColorUtil.toAndroidDisplayColor(((TileChainColors.Fixed) colors).getColor()));
			}
			else {
				TileChain tileChain = (TileChain) DeviceRegistry.getInstance().getDeviceById(storedColor.getDeviceId()).getDevice();
				if (tileChain.getTotalWidth() == 0 || tileChain.getTotalHeight() == 0) {
					drawable.setShape(GradientDrawable.RECTANGLE);
					drawable.setColor(Color.GRAY);
				}
				else {
					Bitmap bitmap = Bitmap.createBitmap(tileChain.getTotalWidth(), tileChain.getTotalHeight(), Config.ARGB_8888);
					for (int y = 0; y < tileChain.getTotalHeight(); y++) {
						for (int x = 0; x < tileChain.getTotalWidth(); x++) {
							bitmap.setPixel(x, tileChain.getTotalHeight() - 1 - y,
									ColorUtil.toAndroidDisplayColor(colors.getColor(x, y, tileChain.getTotalWidth(), tileChain.getTotalHeight())));
						}
					}
					return new BitmapDrawable(Application.getAppContext().getResources(), bitmap);
				}
			}
		}
		return drawable;
	}

	@Override
	public final int getItemCount() {
		return mStoredColors.size();
	}

	@Override
	public final void onRowMoved(final int fromPosition, final int toPosition) {
		if (fromPosition < toPosition) {
			for (int i = fromPosition; i < toPosition; i++) {
				Collections.swap(mStoredColors, i, i + 1);
				Collections.swap(mColorIds, i, i + 1);
			}
		}
		else {
			for (int i = fromPosition; i > toPosition; i--) {
				Collections.swap(mStoredColors, i, i - 1);
				Collections.swap(mColorIds, i, i - 1);
			}
		}
		PreferenceUtil.setSharedPreferenceIntList(R.string.key_color_ids, mColorIds);
		notifyItemMoved(fromPosition, toPosition);
	}

	@Override
	public final void onRowSelected(final MyViewHolder myViewHolder) {
		myViewHolder.mRowView.setBackgroundColor(android.graphics.Color.LTGRAY);

	}

	@Override
	public final void onRowClear(final MyViewHolder myViewHolder) {
		myViewHolder.mRowView.setBackgroundColor(android.graphics.Color.TRANSPARENT);

	}

	/**
	 * The view holder of the items.
	 */
	public class MyViewHolder extends RecyclerView.ViewHolder {
		/**
		 * The whole item.
		 */
		private final View mRowView;
		/**
		 * The title.
		 */
		private final TextView mTitle;
		/**
		 * The device name.
		 */
		private final TextView mDeviceName;
		/**
		 * The button to apply the color.
		 */
		private final ImageView mApplyColorButton;
		/**
		 * The image view.
		 */
		private final ImageView mDragHandle;
		/**
		 * The delete button.
		 */
		private final ImageView mDeleteButton;

		/**
		 * Constructor.
		 *
		 * @param itemView The item view.
		 */
		public MyViewHolder(final View itemView) {
			super(itemView);
			mRowView = itemView;
			mTitle = itemView.findViewById(R.id.textViewColorName);
			mDeviceName = itemView.findViewById(R.id.textViewDeviceName);
			mDragHandle = itemView.findViewById(R.id.imageViewDragHandle);
			mDeleteButton = itemView.findViewById(R.id.imageViewDelete);
			mApplyColorButton = itemView.findViewById(R.id.imageViewApplyColor);
		}
	}

	/**
	 * A listener for starting the drag.
	 */
	public interface StartDragListener {
		/**
		 * Method for starting the drag.
		 *
		 * @param viewHolder The view Holder.
		 */
		void requestDrag(RecyclerView.ViewHolder viewHolder);
	}

	/**
	 * Orientation of the multizone device.
	 */
	public enum MultizoneOrientation {
		/**
		 * Direction left to right.
		 */
		LEFT_TO_RIGHT(Orientation.LEFT_RIGHT, R.drawable.ic_button_arrow_right),
		/**
		 * Direction right to left.
		 */
		RIGHT_TO_LEFT(Orientation.RIGHT_LEFT, R.drawable.ic_button_arrow_left),
		/**
		 * Direction top to bottom.
		 */
		TOP_TO_BOTTOM(Orientation.TOP_BOTTOM, R.drawable.ic_button_arrow_downward),
		/**
		 * Direction bottom to top.
		 */
		BOTTOM_TO_TOP(Orientation.BOTTOM_TOP, R.drawable.ic_button_arrow_upward);

		/**
		 * The gradient orientation.
		 */
		private Orientation mGradientOrientation;
		/**
		 * The button resource used for displaying the orientation.
		 */
		private int mButtonResource;

		/**
		 * Constructor.
		 *
		 * @param gradientOrientation The gradient orientation.
		 * @param buttonResource      The button resource.
		 */
		MultizoneOrientation(final Orientation gradientOrientation, final int buttonResource) {
			mGradientOrientation = gradientOrientation;
			mButtonResource = buttonResource;
		}

		/**
		 * Get the corresponding gradient orientation.
		 *
		 * @return The gradient orientation.
		 */
		public Orientation getGradientOrientation() {
			return mGradientOrientation;
		}

		/**
		 * Get the corresponding button resource used for displaying the orientation.
		 *
		 * @return The button resource
		 */
		public int getButtonResource() {
			return mButtonResource;
		}

		/**
		 * Get the multizone orientation from its ordinal.
		 *
		 * @param ordinal The ordinal.
		 * @return The multizone orientation.
		 */
		public static MultizoneOrientation fromOrdinal(final int ordinal) {
			for (MultizoneOrientation multizoneOrientation : values()) {
				if (multizoneOrientation.ordinal() == ordinal) {
					return multizoneOrientation;
				}
			}
			return LEFT_TO_RIGHT;
		}

	}

}
