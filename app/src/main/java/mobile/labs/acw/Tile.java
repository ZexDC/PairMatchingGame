package mobile.labs.acw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

public class Tile {
    public int gridPosition;

    private int x, y;
    private Bitmap image;
    private Bitmap originalImage;
    private Bitmap backImage;
    private Bitmap selectedImage;
    private String imageName;
    private boolean isEnabled;

    public Tile(Context context, int pX, int pY, Bitmap pImage, String pImageName) {
        x=pX;
        y=pY;
        originalImage=pImage;
        imageName=pImageName;
        isEnabled=true;
        Drawable d = ContextCompat.getDrawable(context, R.drawable.ic_card_back);
        backImage= ((BitmapDrawable) d).getBitmap();
        d = ContextCompat.getDrawable(context, R.drawable.ic_card_back_selected);
        selectedImage=((BitmapDrawable) d).getBitmap();
        image=backImage;
    }

    public int X() {
        return x;
    }

    public int Y() {
        return y;
    }

    public Bitmap Image() {
        return image;
    }

    public String ImageName() {
        return imageName;
    }

    public void setEnabled(boolean value){
        isEnabled=value;
        if(isEnabled) {
            image = backImage;
        }
        else {
            image = originalImage;
        }
    }

    public void setSelected(boolean value){
        if(value) {
            image = selectedImage;
        }
        else {
            image = backImage;
        }
    }

    public boolean isEnabled(){
        return isEnabled;
    }
}
