# Litematica Material Filter

Addon for **Litematica**

Allows filtering the material list by query and displaying only the necessary blocks in the **Info HUD** or in the Schematic Render in the world - without manually clicking "Ignore" on every extra material.

---

<img width="957" height="1011" alt="image" src="https://github.com/user-attachments/assets/856ccd69-b1eb-4fbc-ad17-3283189f4fbb" />

## Usage

1. Load the schematic in Litematica, then select **`Material List`**, where you will find the **`Open Filter`** button, or simply press the **`N`** key by default.
2. The filter screen will appear:
   - Enter a query, for example, `oak` — the list will immediately show only blocks with "oak" in their name. You can apply the entire list to the filter at once by clicking **`Apply to HUD`** or **`Apply to Render`**.
   - You can also simply click to select what you need (**`LMB`** selects the filter for the Material List, **`RMB`** selects the filter for rendering blocks in the world on the schematic).
   - **`Render Filter`** toggles the use of the filter for filtering schematic render blocks in the world.
   - **`Render Mode`** can work as either a whitelist or a blacklist.
   - **`Entities`** toggles the display of entities and fluid animations on the schematic.
     The screenshot shows the filter whitelist enabled for the redstone block.
     <img width="820" height="427" alt="image" src="https://github.com/user-attachments/assets/0225f036-934a-441b-84cc-c9d4f368174c" />

3. **`Material HUD`** enables the hud with the Material List, while **`Raw HUD`** enables the hud with the Raw Material List that can be broken down into base crafting materials.

<img width="456" height="278" alt="image" src="https://github.com/user-attachments/assets/9342eb43-1f92-4d40-874a-d62746cba2ac" />
<img width="488" height="274" alt="image" src="https://github.com/user-attachments/assets/3fbebafd-9c9b-49f8-91e0-6471bfe16f8a" />
<img width="479" height="278" alt="image" src="https://github.com/user-attachments/assets/3d19c567-f75d-457b-8746-d57b292ff7f9" />

4. With **`Raw HUD`** enabled, pressing **`[+]`** breaks down the given material into its components. You can either fully expand all materials or collapse them.

<img width="951" height="1012" alt="image" src="https://github.com/user-attachments/assets/81187e81-a5ca-4d5b-a65d-3ac9675fa998" />

5. To remove the filter, press **`✕ Clear Filter`**.
