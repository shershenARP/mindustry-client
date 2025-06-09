package mindustry.ui.dialogs;
/*
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.scene.ui.Dialog;
import arc.scene.ui.Image;
import arc.scene.event.Touchable;
import mindustry.gen.Icon;

import java.lang.reflect.Field;
import java.util.Arrays;

public class IconPickerDialog extends Dialog {

    public interface IconSelectListener {
        void onSelect(TextureRegionDrawable icon);
    }

    public IconPickerDialog(String title, IconSelectListener listener) {
        super(title);

        Field[] fields = Icon.class.getDeclaredFields();
        Arrays.sort(fields, (a, b) -> a.getName().compareTo(b.getName()));

        Table content = this.cont;

        for (Field f : fields) {
            if (f.getName().endsWith("Small")) continue;

            TextureRegionDrawable region = null;
            try {
                region = (TextureRegionDrawable) f.get(null);
            } catch (Throwable ignored) {
            }
            if (region == null) continue;

            content.row();
            content.table(t -> {
                Cell<Image> imgCell = t.image(region.getRegion()).size(32f);
                imgCell.touchable(Touchable.enabled);

                Image img = imgCell.get();
                img.touchable = Touchable.enabled;
                img.clicked(() -> {
                    listener.onSelect(region);
                    hide();
                });

            }).left().growX();
        }

        buttons.defaults().size(120f, 50f);
        buttons.button("Close", this::hide);
        buttons.row();
    }
}
*/

