"""Very small desktop editor for MHP3rd map node coordinates."""
from __future__ import annotations

import json
import re
import sys
import tkinter as tk
from pathlib import Path
from tkinter import filedialog, messagebox, ttk

from PIL import Image, ImageTk


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
REGISTRY = ROOT / "app/src/main/java/com/waillio/mhp3rdcompanion/MapRegistry.kt"
IMAGES = ROOT / "app/src/main/res/drawable-nodpi"
WORKSPACE = HERE / "map-calibration-workspace.json"
CATEGORIES = [
    "MINING", "BUGS", "BONES", "PLANTS", "MUSHROOMS", "WHETSTONE",
    "HONEY", "BERRIES", "FISH", "MISC", "SPIDER_WEB",
]
COLORS = {
    "MINING": "#607d8b", "BUGS": "#7b5bbe", "BONES": "#e8e1cc",
    "PLANTS": "#4d8c45", "MUSHROOMS": "#b05b4a", "WHETSTONE": "#e7c34d",
    "HONEY": "#d99522", "BERRIES": "#b43e57", "FISH": "#397ead",
    "MISC": "#8a6f55", "SPIDER_WEB": "#8d8d9b",
}


def load_registry() -> dict:
    text = REGISTRY.read_text(encoding="utf-8")
    # MapRegistry now keeps node lists in MapNodeData.nodes rather than
    # private list variables. Keep the old form as a fallback so the tool
    # remains usable with an older checkout as well.
    definitions = re.findall(
        r'definition\("([^"]+)",\s*"([^"]+)",\s*R\.drawable\.([A-Za-z0-9_]+),\s*'
        r'(?:MapNodeData\.nodes\.getValue\("([^"]+)"\)|([A-Za-z0-9_]+))\)',
        text,
    )
    result = {"formatVersion": 1, "maps": []}
    node_data_text = (ROOT / "app/src/main/java/com/waillio/mhp3rdcompanion/MapNodeData.kt").read_text(
        encoding="utf-8"
    )
    for map_id, title, image_name, map_key, variable in definitions:
        if map_key:
            # Each map entry is a top-level `"id" to listOf(...)` value.
            # Stop at that list's closing line, before the next map entry.
            match = re.search(
                rf'^\s*"{re.escape(map_key)}"\s+to\s+listOf\((.*?)'
                rf'(?=^\s*\),?\s*$)',
                node_data_text,
                re.S | re.M,
            )
        else:
            match = re.search(rf"private val {re.escape(variable)} = listOf\((.*?)\n\s*\)", text, re.S)
        if not match:
            raise ValueError(f"Не найден список {map_key or variable}")
        nodes = []
        pattern = re.compile(
            r'(?:node|MapNode)\("([^"]+)",\s*MapNodeCategory\.([A-Z_]+),\s*'
            r'([.\d]+)f,\s*([.\d]+)f,\s*'
            r'(?:areaNumber\s*=\s*)?"([^"]+)"'
            r'(?:,\s*(?:label\s*=\s*)?"([^"]+)")?\)'
        )
        for node_id, category, x, y, area, label in pattern.findall(match.group(1)):
            nodes.append({
                "id": node_id, "category": category, "x": float(x), "y": float(y),
                "area": area, "label": label,
            })
        result["maps"].append({
            "id": map_id, "name": title, "image": f"{image_name}.png", "nodes": nodes,
        })
    return result


def load_data() -> dict:
    if WORKSPACE.exists():
        return json.loads(WORKSPACE.read_text(encoding="utf-8"))
    return load_registry()


def write_json(path: Path, data: dict) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


class Editor(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("MHP3rd Map Calibrator — тупой простой редактор точек")
        self.geometry("1450x900")
        self.minsize(1000, 650)
        self.data = load_data()
        self.current_map = self.data["maps"][0]
        self.selected: dict | None = None
        self.zoom = 3.5
        self.offset_x = 40.0
        self.offset_y = 40.0
        self.pan_start: tuple[int, int, float, float] | None = None
        self.dragging = False
        self.add_mode = False
        self.syncing_selection = False
        self.photo = None
        self.base_image = None
        self.status = tk.StringVar(value="Готово")
        self.vars = {key: tk.StringVar() for key in ("id", "category", "area", "label", "x", "y")}
        self._build_ui()
        self._load_map(self.current_map["id"])
        self.protocol("WM_DELETE_WINDOW", self._close)

    def _build_ui(self) -> None:
        top = ttk.Frame(self, padding=6)
        top.pack(fill="x")
        ttk.Label(top, text="Карта:").pack(side="left")
        self.map_box = ttk.Combobox(top, state="readonly", width=22,
                                    values=[m["name"] for m in self.data["maps"]])
        self.map_box.pack(side="left", padx=5)
        self.map_box.bind("<<ComboboxSelected>>", lambda _e: self._load_map_by_name())
        ttk.Button(top, text="Сохранить workspace", command=self.save).pack(side="left", padx=3)
        ttk.Button(top, text="Экспорт JSON…", command=self.export).pack(side="left", padx=3)
        ttk.Button(top, text="Сбросить из MapRegistry", command=self.reload_source).pack(side="left", padx=20)
        ttk.Label(top, textvariable=self.status).pack(side="right")

        body = ttk.Panedwindow(self, orient="horizontal")
        body.pack(fill="both", expand=True)
        center = ttk.Frame(body)
        right = ttk.Frame(body, padding=8, width=330)
        body.add(center, weight=4)
        body.add(right, weight=1)

        self.canvas = tk.Canvas(center, bg="#17130f", highlightthickness=0, cursor="arrow")
        self.canvas.pack(fill="both", expand=True)
        self.canvas.bind("<MouseWheel>", self.on_wheel)
        self.canvas.bind("<Button-4>", lambda e: self.zoom_at(e.x, e.y, 1.2))
        self.canvas.bind("<Button-5>", lambda e: self.zoom_at(e.x, e.y, 1 / 1.2))
        self.canvas.bind("<ButtonPress-2>", self.start_pan)
        self.canvas.bind("<B2-Motion>", self.pan)
        self.canvas.bind("<ButtonPress-3>", self.start_pan)
        self.canvas.bind("<B3-Motion>", self.pan)
        self.canvas.bind("<ButtonRelease-2>", lambda _e: self.stop_pan())
        self.canvas.bind("<ButtonRelease-3>", lambda _e: self.stop_pan())
        self.canvas.bind("<ButtonPress-1>", self.left_down)
        self.canvas.bind("<B1-Motion>", self.left_drag)
        self.canvas.bind("<ButtonRelease-1>", lambda _e: self.stop_drag())

        ttk.Label(right, text="Ноды", font=("Segoe UI", 13, "bold")).pack(anchor="w")
        buttons = ttk.Frame(right)
        buttons.pack(fill="x", pady=5)
        ttk.Button(buttons, text="+ Добавить точку", command=self.begin_add).pack(side="left")
        ttk.Button(buttons, text="Удалить", command=self.delete_selected).pack(side="left", padx=5)

        self.tree = ttk.Treeview(right, columns=("area", "type"), show="tree headings", height=15)
        self.tree.heading("#0", text="ID")
        self.tree.heading("area", text="Зона")
        self.tree.heading("type", text="Тип")
        self.tree.column("#0", width=165)
        self.tree.column("area", width=45, anchor="center")
        self.tree.column("type", width=90)
        self.tree.pack(fill="both", expand=True)
        self.tree.bind("<<TreeviewSelect>>", self.tree_select)

        form = ttk.LabelFrame(right, text="Выбранная нода", padding=8)
        form.pack(fill="x", pady=8)
        for row, (key, label) in enumerate((
            ("id", "ID"), ("category", "Тип"), ("area", "Зона"), ("label", "Точное имя"),
            ("x", "X (0..1)"), ("y", "Y (0..1)"),
        )):
            ttk.Label(form, text=label).grid(row=row, column=0, sticky="w", pady=2)
            if key == "category":
                widget = ttk.Combobox(form, textvariable=self.vars[key], values=CATEGORIES, state="readonly")
            else:
                widget = ttk.Entry(form, textvariable=self.vars[key])
            widget.grid(row=row, column=1, sticky="ew", pady=2)
        form.columnconfigure(1, weight=1)
        ttk.Button(form, text="Применить поля", command=self.apply_fields).grid(
            row=6, column=0, columnspan=2, sticky="ew", pady=(8, 0)
        )

        ttk.Label(right, text=(
            "ЛКМ по ноде — выбрать и таскать\n"
            "Колесо — приблизить под курсором\n"
            "ПКМ/средняя — двигать карту\n"
            "+ Добавить → клик по карте\n"
            "Ctrl+S — сохранить, Delete — удалить"
        ), justify="left").pack(anchor="w")
        self.bind("<Control-s>", lambda _e: self.save())
        self.bind("<Delete>", lambda _e: self.delete_selected())

    def _load_map_by_name(self) -> None:
        name = self.map_box.get()
        self._load_map(next(m["id"] for m in self.data["maps"] if m["name"] == name))

    def _load_map(self, map_id: str) -> None:
        self.current_map = next(m for m in self.data["maps"] if m["id"] == map_id)
        self.map_box.set(self.current_map["name"])
        image = Image.open(IMAGES / self.current_map["image"]).convert("RGBA")
        self.base_image = image.crop((0, 0, 170, 170))
        self.selected = None
        self.offset_x = self.offset_y = 40
        self.zoom = 3.5
        self.refresh_tree()
        self.redraw()

    def redraw(self) -> None:
        self.canvas.delete("all")
        if self.base_image is None:
            return
        size = max(1, round(170 * self.zoom))
        scaled = self.base_image.resize((size, size), Image.Resampling.NEAREST)
        self.photo = ImageTk.PhotoImage(scaled)
        self.canvas.create_image(self.offset_x, self.offset_y, anchor="nw", image=self.photo)
        radius = max(6, min(14, 4 * self.zoom))
        for index, node in enumerate(self.current_map["nodes"], 1):
            x, y = self.to_canvas(node["x"], node["y"])
            selected = node is self.selected
            self.canvas.create_oval(x-radius, y-radius, x+radius, y+radius,
                                    fill=COLORS.get(node["category"], "#888"),
                                    outline="#ffe17d" if selected else "white", width=4 if selected else 2)
            self.canvas.create_text(x, y, text=str(index), fill="black" if node["category"] == "BONES" else "white",
                                    font=("Segoe UI", max(7, round(radius * .8)), "bold"))
            if selected:
                name = node.get("label") or node["category"].title()
                self.canvas.create_text(x + radius + 5, y - radius, anchor="sw",
                                        text=f'{node["id"]}\n{name} · Area {node["area"]}',
                                        fill="#ffe17d", font=("Segoe UI", 10, "bold"))

    def to_canvas(self, x: float, y: float) -> tuple[float, float]:
        size = 170 * self.zoom
        return self.offset_x + x * size, self.offset_y + y * size

    def from_canvas(self, x: float, y: float) -> tuple[float, float]:
        size = 170 * self.zoom
        return ((x - self.offset_x) / size, (y - self.offset_y) / size)

    def nearest(self, x: int, y: int) -> dict | None:
        limit = max(12, 6 * self.zoom) ** 2
        candidates = []
        for node in self.current_map["nodes"]:
            nx, ny = self.to_canvas(node["x"], node["y"])
            candidates.append(((nx-x) ** 2 + (ny-y) ** 2, node))
        best = min(candidates, default=(limit + 1, None), key=lambda item: item[0])
        return best[1] if best[0] <= limit else None

    def select(self, node: dict | None) -> None:
        if self.syncing_selection:
            return
        self.syncing_selection = True
        self.selected = node
        try:
            if node:
                for key in self.vars:
                    value = node.get(key, "")
                    self.vars[key].set(f"{value:.4f}" if key in ("x", "y") else value)
                if self.tree.selection() != (node["id"],):
                    self.tree.selection_set(node["id"])
                self.tree.see(node["id"])
            else:
                self.tree.selection_remove(self.tree.selection())
                for var in self.vars.values():
                    var.set("")
            self.redraw()
        finally:
            self.syncing_selection = False

    def left_down(self, event) -> None:
        if self.add_mode:
            x, y = self.from_canvas(event.x, event.y)
            if not (0 <= x <= 1 and 0 <= y <= 1):
                self.status.set("Кликните внутри карты")
                return
            node = self.new_node(x, y)
            self.current_map["nodes"].append(node)
            self.add_mode = False
            self.canvas.configure(cursor="arrow")
            self.refresh_tree()
            self.select(node)
            self.status.set("Нода добавлена — исправьте поля справа")
            return
        node = self.nearest(event.x, event.y)
        self.select(node)
        self.dragging = node is not None

    def left_drag(self, event) -> None:
        if not self.dragging or not self.selected:
            return
        x, y = self.from_canvas(event.x, event.y)
        self.selected["x"] = round(max(0, min(1, x)), 4)
        self.selected["y"] = round(max(0, min(1, y)), 4)
        self.vars["x"].set(f'{self.selected["x"]:.4f}')
        self.vars["y"].set(f'{self.selected["y"]:.4f}')
        self.redraw()

    def stop_drag(self) -> None:
        self.dragging = False

    def start_pan(self, event) -> None:
        self.pan_start = (event.x, event.y, self.offset_x, self.offset_y)

    def pan(self, event) -> None:
        if self.pan_start:
            x, y, ox, oy = self.pan_start
            self.offset_x, self.offset_y = ox + event.x - x, oy + event.y - y
            self.redraw()

    def stop_pan(self) -> None:
        self.pan_start = None

    def on_wheel(self, event) -> None:
        self.zoom_at(event.x, event.y, 1.2 if event.delta > 0 else 1 / 1.2)

    def zoom_at(self, x: int, y: int, factor: float) -> None:
        old = self.zoom
        new = max(.5, min(12, old * factor))
        if new == old:
            return
        map_x, map_y = self.from_canvas(x, y)
        self.zoom = new
        self.offset_x = x - map_x * 170 * new
        self.offset_y = y - map_y * 170 * new
        self.redraw()

    def begin_add(self) -> None:
        self.add_mode = True
        self.canvas.configure(cursor="crosshair")
        self.status.set("Кликните по карте, где должна быть новая нода")

    def new_node(self, x: float, y: float) -> dict:
        prefix = self.current_map["id"].split("_")[0][:2]
        number = 1
        existing = {node["id"] for node in self.current_map["nodes"]}
        while f"{prefix}-new-{number}" in existing:
            number += 1
        return {"id": f"{prefix}-new-{number}", "category": "MISC", "x": round(x, 4),
                "y": round(y, 4), "area": "?", "label": ""}

    def apply_fields(self) -> None:
        if not self.selected:
            return
        old_id = self.selected["id"]
        try:
            x, y = float(self.vars["x"].get()), float(self.vars["y"].get())
            if not (0 <= x <= 1 and 0 <= y <= 1):
                raise ValueError
        except ValueError:
            messagebox.showerror("Ошибка", "X и Y должны быть числами от 0 до 1")
            return
        new_id = self.vars["id"].get().strip()
        if not new_id or any(n is not self.selected and n["id"] == new_id for n in self.current_map["nodes"]):
            messagebox.showerror("Ошибка", "ID пустой или уже занят")
            return
        self.selected.update(id=new_id, category=self.vars["category"].get(),
                             area=self.vars["area"].get().strip(), label=self.vars["label"].get().strip(),
                             x=round(x, 4), y=round(y, 4))
        self.refresh_tree()
        self.select(self.selected)
        self.status.set(f"Применено: {old_id} → {new_id}")

    def delete_selected(self) -> None:
        if not self.selected:
            return
        node_id = self.selected["id"]
        if messagebox.askyesno("Удалить", f"Удалить {node_id}?"):
            self.current_map["nodes"].remove(self.selected)
            self.select(None)
            self.refresh_tree()
            self.status.set(f"Удалено: {node_id}")

    def refresh_tree(self) -> None:
        for item in self.tree.get_children():
            self.tree.delete(item)
        for node in self.current_map["nodes"]:
            self.tree.insert("", "end", iid=node["id"], text=node["id"],
                             values=(node["area"], node["category"]))

    def tree_select(self, _event=None) -> None:
        if self.syncing_selection:
            return
        ids = self.tree.selection()
        if ids:
            node = next(node for node in self.current_map["nodes"] if node["id"] == ids[0])
            if node is not self.selected:
                self.select(node)

    def save(self) -> None:
        write_json(WORKSPACE, self.data)
        self.status.set(f"Сохранено: {WORKSPACE.name}")

    def export(self) -> None:
        path = filedialog.asksaveasfilename(initialdir=HERE, initialfile="mhp3rd-map-nodes-edited.json",
                                            defaultextension=".json", filetypes=[("JSON", "*.json")])
        if path:
            write_json(Path(path), self.data)
            self.status.set(f"Экспортировано: {path}")

    def reload_source(self) -> None:
        if not messagebox.askyesno("Сбросить всё?", "Удалить несохранённые правки и перечитать MapRegistry.kt?"):
            return
        self.data = load_registry()
        self._load_map(self.data["maps"][0]["id"])
        self.status.set("Данные перечитаны из MapRegistry.kt")

    def _close(self) -> None:
        self.save()
        self.destroy()


def check() -> int:
    data = load_registry()
    print(f"OK: {len(data['maps'])} maps, {sum(len(m['nodes']) for m in data['maps'])} nodes")
    for item in data["maps"]:
        print(f"  {item['id']}: {len(item['nodes'])}")
    return 0


if __name__ == "__main__":
    if "--check" in sys.argv:
        raise SystemExit(check())
    Editor().mainloop()
