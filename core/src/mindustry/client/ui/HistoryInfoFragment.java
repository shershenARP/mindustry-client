package mindustry.client.ui;

import arc.Core;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.HandCursorListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.client.antigrief.TileRecords;
import mindustry.core.ActionsHistory;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Tile;
import java.util.ArrayList;
import java.util.List;


public class HistoryInfoFragment extends Table{
    private boolean hovered = false;
    private boolean pressed = false;

    private static Table reslog = new Table();
    public HistoryInfoFragment() {
        setBackground(Tex.buttonEdge5);
        Image img = new Image();
        add(img);
        Label label = new Label("");
        add(label).height(126);
        visible(() -> Core.settings.getBool("tilehud"));

        ClickListener listener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setBackground(Tex.buttonEdgeDown5);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element toActor) {
                setBackground(Tex.buttonEdge5);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element toActor) {
                setBackground(Tex.buttonEdgeOver5);
            }
        };

        label.addListener(listener);
        label.addListener(new HandCursorListener());

        var builder = new StringBuilder();
        update(() -> {

            var record  = TileRecords.INSTANCE.getHistory();
            if (record.size() < 1 ) return;
            builder.setLength(0);
            for (var item : record) {
                item = item.replace(
                        Core.bundle.get("client.built"),
                        "[#41e89a]" + Core.bundle.get("client.built") + "[]"
                ).replace(
                        Core.bundle.get("client.broke"),
                        "[#f25c5c]" + Core.bundle.get("client.broke") + "[]"
                ).replace(
                        Core.bundle.get("client.rotated"),
                        "[stat]" + Core.bundle.get("client.rotated") + "[]"
                ).replace(
                        Core.bundle.get("client.configured"),
                        "[#7457ce]" + Core.bundle.get("client.configured") + "[]"
                );
                builder.append(item).append("\n");
            }
            label.setText(builder.length() == 0 ? "" : builder.substring(0, builder.length() - 1));
        });

        label.clicked(() -> {
           showreslog();
        });
    }


    //этот код написан чатомгпт, и он даже работает
    //главной проблемой была тут оптимизация
    public static void showreslog() {
        BaseDialog reslogDialog = new BaseDialog("logs");
        reslogDialog.cont.top().left();

        ArrayList<String> fullHistory = TileRecords.INSTANCE.getHistoryShow();
        ArrayList<String> fullHistoryMinor = TileRecords.INSTANCE.getHistoryMinor();

        class LogSection {
            Table wrapper = new Table().top().left(); // обёртка для этой секции
            Table content = new Table().top().left(); // таблица с логами
            ScrollPane scroll;
            TextField searchField = new TextField();
            Button prev = new TextButton("<");
            Button next = new TextButton(">");
            TextField pageInput = new TextField();
            Label maxPageLabel = new Label("");
            Label firstPageLabel = new Label("1");
            int pageSize = 100;
            int currentPage = 0;
            int totalPages = 1;
            ArrayList<String> source;
            List<String> filtered = new ArrayList<>();

            LogSection(ArrayList<String> source) {
                this.source = source;

                scroll = new ScrollPane(content);
                scroll.setScrollingDisabled(false, false);
                scroll.setFadeScrollBars(true);

                searchField.setMessageText("Поиск...");

                // Фильтр ввода только цифр для pageInput
                pageInput.setText("");
                pageInput.setAlignment(Align.center);
                pageInput.setWidth(10);
                pageInput.setFilter(new TextField.TextFieldFilter() {
                    @Override
                    public boolean acceptChar(TextField textField, char c) {
                        return Character.isDigit(c);
                    }
                });

                Runnable update = () -> {
                    content.clear();
                    filtered.clear();
                    String query = searchField.getText().toLowerCase();

                    for(String item : source){
                        if(query.isEmpty() || item.toLowerCase().contains(query)){
                            filtered.add(item);
                        }
                    }

                    totalPages = Math.max(1, (int)Math.ceil(filtered.size() / (float) pageSize));
                    currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

                    int start = currentPage * pageSize;
                    int end = Math.min(filtered.size(), start + pageSize);

                    for (int i = end - 1; i >= start; i--) {
                        content.add(formatLog(filtered.get(i))).left().growX().wrap();
                        content.row();
                    }

                    prev.setDisabled(currentPage == 0);
                    next.setDisabled(currentPage >= totalPages - 1);

                    maxPageLabel.setText(String.valueOf(totalPages));
                    pageInput.setText(String.valueOf(currentPage + 1));
                };

                searchField.changed(() -> {
                    currentPage = 0;
                    update.run();
                });

                prev.clicked(() -> {
                    if(currentPage > 0) {
                        currentPage--;
                        update.run();
                    }
                });

                next.clicked(() -> {
                    if(currentPage < totalPages - 1) {
                        currentPage++;
                        update.run();
                    }
                });

                // Обработка ввода страницы по Enter
                pageInput.setTextFieldListener((field, c) -> {
                    if (c == '\n' || c == '\r') {
                        try {
                            int page = Integer.parseInt(pageInput.getText()) - 1;
                            if(page >= 0 && page < totalPages){
                                currentPage = page;
                                update.run();
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                });

                // wrapper.add(prev).padTop(5);
                // wrapper.add(next).padTop(5);
                // wrapper.row();
                // Table pageNav = new Table();
                // pageNav.defaults().pad(4).align(Align.center);
                // pageNav.add(maxPageLabel);
                // pageNav.add(new Label("..."));
                // pageNav.add(pageInput);
                // pageNav.add(new Label("..."));
                // pageNav.add(firstPageLabel);
                // wrapper.add(pageNav).colspan(5).padTop(5);

                // layout
                wrapper.add(searchField).growX().height(35).padTop(-24).padBottom(4).colspan(5).row();

                wrapper.add(scroll).size(Core.graphics.getWidth()/2f - 30, Core.graphics.getHeight() - 220).colspan(5).row();

                Table bottomBar = new Table();
                bottomBar.defaults().center();

                bottomBar.add(prev).size(40, 30).padRight(-80);
                bottomBar.add(next).size(40, 30).padLeft(50);
                bottomBar.row();

              bottomBar.add().width(10);

                Table pageNav = new Table();
                pageNav.defaults().pad(0, 4, 0, 4).center();

                pageNav.add(firstPageLabel);
                pageNav.add(new Label("..."));
                pageNav.add(pageInput).size(60, 30);
                pageNav.add(new Label("..."));
                pageNav.add(maxPageLabel);

                bottomBar.add(pageNav);

                wrapper.add(bottomBar).colspan(5).row();


                update.run();
            }
        }

        LogSection leftLog = new LogSection(fullHistory);
        LogSection rightLog = new LogSection(fullHistoryMinor);

        // Добавляем обе секции рядом
        reslogDialog.cont.table(t -> {
            t.add(leftLog.wrapper).top().left().pad(10);
            t.add(rightLog.wrapper).top().right().pad(10);
        });

        reslogDialog.buttons.button("@ok", reslogDialog::hide).size(110, 50).pad(4);
        reslogDialog.keyDown(KeyCode.escape, reslogDialog::hide);
        reslogDialog.closeOnBack();
        reslogDialog.show();
    }



    // Вынесенная функция для форматирования текста логов
    private static String formatLog(String item) {
        return item
                .replace(Core.bundle.get("client.built"), "[#41e89a]" + Core.bundle.get("client.built") + "[]")
                .replace(Core.bundle.get("client.broke"), "[#f25c5c]" + Core.bundle.get("client.broke") + "[]")
                .replace(Core.bundle.get("client.rotated"), "[stat]" + Core.bundle.get("client.rotated") + "[]")
                .replace(Core.bundle.get("client.configured"), "[#7457ce]" + Core.bundle.get("client.configured") + "[]")
                .replace(Core.bundle.get("client.command"), "[gray]" + Core.bundle.get("client.command") + "[]");
    }

}