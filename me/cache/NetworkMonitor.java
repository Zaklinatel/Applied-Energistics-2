package appeng.me.cache;

import java.util.LinkedList;
import java.util.Set;

import appeng.api.networking.events.MENetworkStorageEvent;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.MEMonitorHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.storage.ItemWatcher;

public class NetworkMonitor<T extends IAEStack<T>> extends MEMonitorHandler<T>
{

	final private GridStorageCache myGridCache;
	final private StorageChannel myChannel;

	boolean sendEvent = false;

	public void forceUpdate()
	{
		hasChanged = true;
	}

	public NetworkMonitor(GridStorageCache cache, StorageChannel chan) {
		super( null );
		myGridCache = cache;
		myChannel = chan;
	}

	final static public LinkedList depth = new LinkedList();

	private BaseActionSource src;

	public void setSource(BaseActionSource src)
	{
		this.src = src;
	}

	@Override
	protected BaseActionSource getSource()
	{
		return src;
	}

	@Override
	protected void postChange(T diff, BaseActionSource src)
	{
		if ( depth.contains( this ) )
			return;

		depth.push( this );

		sendEvent = true;
		super.postChange( diff, src );

		if ( myGridCache.interests.containsKey( diff ) )
		{
			Set<ItemWatcher> list = myGridCache.interests.get( diff );
			if ( !list.isEmpty() )
			{
				IItemList<T> myStorageList = getStorageList();

				IAEStack fullStack = myStorageList.findPrecise( diff );
				if ( fullStack == null )
				{
					fullStack = diff.copy();
					fullStack.setStackSize( 0 );
				}

				for (ItemWatcher iw : list)
					iw.getHost().onStackChange( myStorageList, fullStack, diff, src, getChannel() );
			}
		}

		Object last = depth.pop();
		if ( last != this )
			throw new RuntimeException( "Invalid Access to Networked Storage API detected." );
	}

	public void onTick()
	{
		if ( sendEvent )
		{
			sendEvent = false;
			myGridCache.myGrid.postEvent( new MENetworkStorageEvent( getStorageList(), myChannel ) );
		}
	}

	@Override
	protected IMEInventoryHandler getHandler()
	{
		switch (myChannel)
		{
		case ITEMS:
			return myGridCache.getItemInventoryHandler();
		case FLUIDS:
			return myGridCache.getFluidInventoryHandler();
		default:
		}
		return null;
	}

}
