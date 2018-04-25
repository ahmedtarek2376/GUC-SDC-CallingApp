package com.guc.ahmed.callingapp.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.guc.ahmed.callingapp.R;

public class CustomMarker extends LinearLayout {
    private TextView markerText;
    private ImageView markerIcon;

    private String text;
    private int image;
    private int color;

    public CustomMarker(Context context) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.custom_marker, this, true);

        markerText = (TextView) findViewById(R.id.marker_txt);
        markerIcon = (ImageView) findViewById(R.id.marker_icon);
    }

    public void setText(String text) {
        this.text = text;
        markerText.setText(text);
    }

    public void setPickupText(){
        markerText.setText("Pickup");
        markerText.setTextSize(24);
        markerText.setTextColor(Color.rgb(255,0,0));
    }

    public void setImage(int image) {
        this.image = image;
        markerIcon.setImageResource(image);
    }

    public void setColor(int color) {
        this.color = color;
        markerIcon.setColorFilter(ContextCompat.getColor(getContext(), color));
    }

    public Bitmap createBitmapFromView() {
        View v = this;
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(v.getMeasuredWidth(),
                v.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(bitmap);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return bitmap;
    }
}

