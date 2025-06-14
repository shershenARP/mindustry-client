package mindustry.client.ui

import arc.Core
import arc.func.Boolp
import arc.graphics.Color
import arc.graphics.g2d.TextureRegion
import arc.scene.style.TextureRegionDrawable
import arc.scene.ui.*
import arc.scene.ui.layout.Table
import arc.util.Strings
import mindustry.FFF.B64
import mindustry.Vars
import mindustry.Vars.ui
import mindustry.client.Main.send
import mindustry.client.communication.MarkerTransmission
import mindustry.client.communication.SchematicTransmission
import mindustry.client.navigation.Markers
import mindustry.client.navigation.clientThread.post
import mindustry.client.utils.row
import mindustry.gen.Icon
import mindustry.gen.Tex
import mindustry.ui.Styles
import mindustry.ui.dialogs.BaseDialog
import mindustry.ui.dialogs.IconPickerDialog
import org.bouncycastle.jcajce.provider.symmetric.ARC4.Base
import kotlin.random.Random

object MarkerDialog : BaseDialog("Markers") {
    val pane = Table()
    init {
        cont.add("Minimap Markers").center().top()
        cont.row()
        cont.pane(pane).width(800f).grow()
        pane.add().width(pane.width)
        cont.row()
        buttons.button(Icon.upload) {
            importMarkers();
        }
        buttons.button(Icon.export) {
            exportMarkers(Markers)
        }.right().pad(5f)
        buttons.button(Icon.add) { MarkerAddDialog().show() }.growX()
        buttons.button("Close") { hide() }.growX()
        addCloseListener()
        updatePane()

        shown { updatePane() }
    }

    private fun updatePane() {
        pane.clearChildren()

        for (marker in Markers) {
            val table = Table()
            table.margin(15f)
            table.width = pane.width
            //table.image(marker.shape).left().pad(5f)//.width(20f)
            table.add(Image(marker.shape)).left().pad(5f).get().clicked {
                val dialog = IconPickerDialog("Select Icon") { icon ->
                    marker.shape = TextureRegionDrawable(icon)
                    updatePane()
                }
                dialog.show()
            }

            /*
            table.stack(Image(Tex.alphaBg), Image(Tex.whiteui).apply {
                update { setColor(marker.color) }
            }).size(50f).pad(5f).left().get().clicked {
                ui.picker.show(marker.color) { color -> marker.color = color }
            }
            */

            table.stack(Image(Tex.alphaBg), Image(Tex.whiteui).apply {
                update { setColor(marker.color) }
            }).size(50f).pad(5f).left().get().clicked {
                ui.picker.show(marker.color) { color ->
                    marker.color = Color(color)
                }
            }

            table.add(marker.name).left().pad(5f).growX().get().clicked {
                ui.showTextInput("Name", "Name", marker.name) {
                    if (it.isNotBlank()) {
                        marker.name = it
                        updatePane()
                    }
                }
            }

            table.add().growX()

            table.add("(${marker.x}, ${marker.y})").right().pad(5f).get().clicked {
                ui.showTextInput("Coordinates", "Coordinates", "${marker.x}, ${marker.y}") {
                    if (it.matches("\\(?\\d+, ?\\d+\\)?".toRegex())) {
                        val matches = "\\d+".toRegex().findAll(it)
                        marker.x = matches.first().value.toInt()
                        marker.y = matches.last().value.toInt()
                        updatePane()
                    }
                }
            }

            table.button(Icon.trash) {
                Markers.remove(marker)
                updatePane()
            }.right().pad(5f)//.width(50f)

            table.button(Icon.export) {
                exportMarkerDialog(marker)
                updatePane()
            }.right().pad(5f)

            pane.row(table).growX()
        }
    }

    class MarkerAddDialog : BaseDialog("Add Marker") {
        init {
            val nameField = TextField("")
            nameField.messageText = "Name"
            cont.row(nameField).maxTextLength(32)

            val xField = cont.field("") {}.valid(Strings::canParsePositiveInt).growX().get()
            xField.messageText = "X"
            val yField = cont.field("") {}.valid(Strings::canParsePositiveInt).growX().get()
            yField.messageText = "Y"

            cont.row()

            var color = Color.HSVtoRGB(Random.nextFloat() * 360, 75f, 75f)

            val image = Image(Icon.star).apply {
                setColor(color)
            }

            cont.stack(Image(Tex.alphaBg), Image(Tex.whiteui).apply {
                update { setColor(color) }
            }).size(50f).pad(5f).left().get().clicked {
                ui.picker.show(color) { col ->
                    color = col
                    image?.setColor(color)
                }
            }
            cont.row()
            cont.add(image).size(60f).left().get().clicked {
                val dialog = IconPickerDialog("Select Icon") { icon ->
                    image.setDrawable(icon)
                    image.setColor(color)
                    //update {
                    //image(icon) угарно вышло
                    //}
                }
                dialog.show()
            }

            buttons.button("Ok") {
                if (xField.isValid && yField.isValid && nameField.text.isNotBlank()) {
                    Markers.add(
                        Markers.Marker(xField.text.toInt(), yField.text.toInt(), nameField.text, color, TextureRegionDrawable(image.region))
                    )
                    updatePane()
                    hide()
                }
            }.growX()

            buttons.button("@back", Icon.left, this::hide).growX()
        }
    }

    fun exportMarkerDialog(marker: Markers.Marker) {
        val dialog = BaseDialog("@editor.export")
        dialog.cont.pane { p: Table ->
            p.margin(10f)
            p.table(Tex.button) { t: Table ->
                val style = Styles.flatt
                t.defaults().size(280f, 60f).left()
                t.button("@schematic.copy", Icon.copy, style) {
                    dialog.hide()
                    ui.showInfoFade("@copied")
                    var text = B64.write("mrks add ${marker.x} ${marker.y} ${marker.color} ${nameFromDrawable(marker.shape)} ${marker.name}")
                    Core.app.setClipboardText(text)
                }.marginLeft(12f)
                t.row()
                t.button("@client.schematic.chatshare", Icon.bookOpen, style) {
                    dialog.hide()
                    post {
                        send(MarkerTransmission(marker)) {
                            Core.app.post {
                                ui.showInfoToast(
                                    Core.bundle["client.finisheduploading"],
                                    2f
                                )
                            }
                        }
                    }
                }.marginLeft(12f).get().setDisabled { !Vars.state.isPlaying }
            }
        }

        dialog.addCloseButton()
        dialog.show()
    }

    fun exportMarkers(markers: Markers) {
        val dialog = BaseDialog("@editor.export")
        dialog.cont.pane { p: Table ->
            p.margin(10f)
            p.table(Tex.button) { t: Table ->
                val style = Styles.flatt
                t.defaults().size(280f, 60f).left()
                t.button("@schematic.copy", Icon.copy, style) {
                    val builder = StringBuilder()
                    builder.appendLine("mrks list")
                    for (marker in markers) {
                        builder.appendLine(" add ${marker.x} ${marker.y} ${marker.color} ${nameFromDrawable(marker.shape)} ${marker.name}")
                    }
                    builder.append("}")

                    //Core.app.setClipboardText(Base32768Coder.encode(builder.toString()))
                    Core.app.setClipboardText(B64.write(builder.toString()))
                    ui.showInfoFade("@copied")
                }.marginLeft(12f)
                t.row()
                t.button("@client.schematic.chatshare", Icon.bookOpen, style) {
                    val markersExportDialog = BaseDialog("@editor.export")
                    val selectedMarkers = mutableMapOf<Markers.Marker, Boolp>() // Marker -> isSelected()

                    val scroll = Table()
                    val pane = ScrollPane(scroll, Styles.smallPane)
                    pane.setScrollingDisabled(true, false)

                    for (marker in Markers) {
                        val row = Table()

                        val selected = Boolp { true }.apply { selectedMarkers[marker] = this }

                        val nameLabel = Label(marker.name)
                        val coordLabel = Label("(${marker.x}, ${marker.y})")
                        val iconImage = Image(marker.shape)

                        val checkbox = CheckBox("").apply {
                            isChecked = true
                            changed {
                                selectedMarkers[marker] = Boolp { isChecked }
                            }
                        }
                        row.add(checkbox).padRight(5f)

                        row.add(iconImage).left().pad(5f).get().clicked {
                            val picker = IconPickerDialog("Select Icon") { icon ->
                                marker.shape = TextureRegionDrawable(icon)
                                iconImage.setDrawable(marker.shape)
                            }
                            picker.show()
                        }

                        val colorBg = Image(Tex.whiteui)
                        row.stack(Image(Tex.alphaBg), colorBg).size(50f).pad(5f).left().get().clicked {
                            ui.picker.show(marker.color) { color ->
                                marker.color = Color(color)
                            }
                        }

                        row.add(nameLabel).left().pad(5f).get().clicked {
                            ui.showTextInput("Name", "Name", marker.name) {
                                if (it.isNotBlank()) {
                                    marker.name = it
                                    nameLabel.setText(it)
                                }
                            }
                        }

                        row.add(coordLabel).get().clicked {
                            ui.showTextInput("Coordinates", "Coordinates", "${marker.x}, ${marker.y}") {
                                if (it.matches("\\(?\\d+, ?\\d+\\)?".toRegex())) {
                                    val matches = "\\d+".toRegex().findAll(it)
                                    marker.x = matches.first().value.toInt()
                                    marker.y = matches.last().value.toInt()
                                    coordLabel.setText("(${marker.x}, ${marker.y})")
                                }
                            }
                        }

                        row.update {
                            colorBg.setColor(marker.color)
                            nameLabel.setText(marker.name)
                            coordLabel.setText("(${marker.x}, ${marker.y})")
                        }

                        scroll.add(row).growX().pad(4f)
                        scroll.row()
                    }

                    markersExportDialog.cont.add(pane).maxHeight(300f).padBottom(10f)
                    markersExportDialog.cont.row()

                    val buttonTable = Table()

                    buttonTable.button("@editor.export", Icon.upload) {
                        val exportList = selectedMarkers.filter { it.value.get() }.map { it.key }
                        send(MarkerTransmission(exportList)) {
                            Core.app.post {
                                ui.showInfoToast(
                                    Core.bundle["client.finisheduploading"],
                                    2f
                                )
                            }
                        }
                        markersExportDialog.hide()
                        dialog.hide()
                    }.growX().padBottom(5f)

                    buttonTable.row()
                    buttonTable.button("@back", Icon.left, markersExportDialog::hide).size(210f, 64f)

                    markersExportDialog.buttons.add(buttonTable).growX()
                    markersExportDialog.closeOnBack()
                    markersExportDialog.show()
                }.marginLeft(12f).get().setDisabled { !Vars.state.isPlaying }
            }
        }

        dialog.addCloseButton()
        dialog.show()
    }

    fun importMarkers() {
        if (Core.app.getClipboardText().isNotEmpty()) {
            try {
                //var text = Base32768Coder.decodeString(Core.app.getClipboardText());
                var text = B64.read(Core.app.getClipboardText());
                if (text.startsWith("mrks add")) {
                    var clipboardText = text.split(' ')
                    if (clipboardText.size >= 6) {
                        var x = clipboardText[2].toInt()
                        var y = clipboardText[3].toInt()
                        var color = clipboardText[4]
                        var shape = Icon.icons.get(clipboardText[5])
                        var name = clipboardText.subList(6, clipboardText.size).joinToString(" ")

                        try {
                            Markers.add(Markers.Marker(x, y, name, Color.valueOf(color), shape))
                        } catch (e: Throwable) {
                            ui.showErrorMessage("Invalid color/shap: ${color} ${shape}")
                        }

                        updatePane()
                    }
                }

                if (text.startsWith("mrks list") && text.endsWith("}")) {
                    val lines = text.lines()
                        .map { it.trim() }
                        .filter { it.startsWith("add") }

                    for (line in lines) {
                        val parts = line.split(" ")

                        if (parts.size >= 5) {
                            val x = parts[1].toIntOrNull() ?: continue
                            val y = parts[2].toIntOrNull() ?: continue
                            val colorHex = parts[3]
                            var shape = Icon.icons.get(parts[4])
                            val name = parts.subList(5, parts.size).joinToString(" ")

                            try {
                                Markers.add(Markers.Marker(x, y, name, Color.valueOf(colorHex), shape))
                            } catch (e: Throwable) {
                                ui.showErrorMessage("Invalid color/shap: ${colorHex} ${shape}")
                            }
                        }
                    }
                    updatePane()
                }
            } catch (e: Throwable) {
                ui.showErrorMessage("Invalid")
            }
        }
    }
    //дальше кромешный ад
    //*рабочий кромешный ад
    fun regionsEqual(a: TextureRegion, b: TextureRegion): Boolean {
        return a.texture == b.texture &&
                a.x == b.x &&
                a.y == b.y &&
                a.width == b.width &&
                a.height == b.height
    }

    fun nameFromDrawable(drawable: TextureRegionDrawable): String? {
        val region = drawable.region
        return Icon.icons.entries().find { regionsEqual(it.value.region, region) }?.key
    }
    //сам в шоке, котлин - ужасен, а фус на нём полностью написан. Это ли не печально?
}
