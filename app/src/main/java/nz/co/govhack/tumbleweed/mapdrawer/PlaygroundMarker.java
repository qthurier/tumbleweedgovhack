package nz.co.govhack.tumbleweed.mapdrawer;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;


public class PlaygroundMarker implements ClusterItem {
    private final LatLng mPosition;
    private String mName;
    private int mItems;
    private String mAddress;
    private int threshold1 = 4;
    private int threshold2 = 8;

    public PlaygroundMarker(double lat, double lng, String name, int items, String address) {
        mPosition = new LatLng(lat, lng);
        mName = name;
        mItems = items;
        mAddress = address;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }


    public BitmapDescriptor getIcon() {
        if(mItems >= threshold2) {
            return BitmapDescriptorFactory.fromResource(R.drawable.icon_playground_40_pct);
        } else if(mItems < threshold2 &&  mItems >= threshold1) {
            return BitmapDescriptorFactory.fromResource(R.drawable.icon_playground_30_pct);
        } else {
            return BitmapDescriptorFactory.fromResource(R.drawable.icon_playground_20_pct);
        }
    }

    public String getSnippet() {
        if(mName.length() <= mAddress.length()) {
            return mAddress.substring(0, mName.length()) + "...";
        } else {
            return mAddress;
        }
    }

    public String getTitle() {
        return mName;
    }

}


