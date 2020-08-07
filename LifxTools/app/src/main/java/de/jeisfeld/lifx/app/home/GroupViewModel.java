package de.jeisfeld.lifx.app.home;

import android.content.Context;
import android.text.Html;

import androidx.core.text.HtmlCompat;
import de.jeisfeld.lifx.lan.Group;

/**
 * Class holding data for the display view of a group.
 */
public class GroupViewModel extends MainViewModel {
	/**
	 * The group.
	 */
	private final Group mGroup;

	/**
	 * Constructor.
	 *
	 * @param context the context.
	 * @param group   The group.
	 */
	public GroupViewModel(final Context context, final Group group) {
		super(context);
		mGroup = group;
	}

	/**
	 * Get the group.
	 *
	 * @return the group.
	 */
	protected Group getGroup() {
		return mGroup;
	}

	@Override
	public final CharSequence getLabel() {
		return Html.fromHtml("<b>" + mGroup.getGroupLabel() + "</b>", HtmlCompat.FROM_HTML_MODE_LEGACY);
	}

	@Override
	public final void togglePower() {
		// TODO
	}
}
