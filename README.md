# ⚔️ WeaponSkin

A Minecraft plugin that lets players apply and remove cosmetic skins on weapons — without replacing the item itself.

> Supports **Oraxen** and a fully **built-in resource pack system** (no dependencies required).

---

## ✨ Features

- Apply custom weapon skins via drag-and-drop (inventory interaction)
- Remove skins with a dedicated **Skin Remover** item
- Preview skins before applying with **Shift + Right Click**
- Built-in resource pack builder, HTTP host server, and auto-push to players
- Supports **Oraxen** as an alternative provider
- `auto` provider detection — uses Oraxen if available, falls back to built-in automatically
- **Multi-language support** — configurable language files (English, Vietnamese)
- **Granular permissions** — individual permission nodes for each sub-command

---

## 📚 Documentation

Full documentation is available at: **[https://k4han.github.io/WeaponSkin/](https://k4han.github.io/WeaponSkin/)**

---

## 📋 Requirements

| Dependency | Type | Notes |
|---|---|---|
| [PacketEvents](https://github.com/retrooper/packetevents) | Required | For equipment packet handling |
| [Oraxen](https://oraxen.com/) | Optional | Only needed if `provider: oraxen` |
| Minecraft | 1.21.4-1.21.11, 26.1.1-26.1.2 | API version `1.21` for one-jar compatibility |
| Java | 21+ / 25 | Java 21 for Paper 1.21.x; Java 25 required for Paper 26.1+ |

---

## 🚀 Installation

1. Download `WeaponSkin.jar` and place it in your `plugins/` folder
2. Download [PacketEvents](https://github.com/retrooper/packetevents/releases) and place it in `plugins/` as well
3. Start the server — config files will be generated automatically
4. Edit `plugins/WeaponSkin/config.yml` and `skins.yml`
5. Run `/weaponskin pack build` (or `/ws pack build`) to generate and host the resource pack

---

## ⚙️ Configuration

### `config.yml`

```yaml
# Prefix for all plugin messages
prefix: "&7[&eWeaponSkin&7] "

# ============================================
# Update Checker
# ============================================
# Automatically check for updates on startup
update-checker: true

# ============================================
# Language Configuration
# ============================================
# Language setting: en | vi
language: en

# Sounds for applying/removing skins (global, no per-skin override)
apply-sound: ENTITY_PLAYER_LEVELUP
remove-sound: ENTITY_ITEM_BREAK

# Provider: auto | oraxen | item_model
provider: auto

# Resource Pack metadata (used by built-in pack builder)
pack:
  description: "WeaponSkin resource pack"
  namespace: "weaponskin"

# Auto-Host Configuration (only used when provider = item_model)
host:
  enabled: true          # Enable/disable HTTP server
  required: true         # Whether clients MUST accept the resource pack
  type: "self-host"      # self-host | external-host

  self-host:
    ip: "localhost"      # Public IP or domain of your server
    port: 8765

  external-host:
    url: "https://example.com/packs"
```

### `skins.yml`

```yaml
rivers_of_blood:
  model_id: riversofblood_sword
  allowed_materials:
    - DIAMOND_SWORD
    - NETHERITE_SWORD
```

> Each skin requires a `model_id` (matching a key in `skins/*/items.yml`) and a list of `allowed_materials`.

---

## 📦 Provider Modes

| Provider | Description |
|---|---|
| `auto` | Uses Oraxen if installed, otherwise falls back to `item_model` |
| `item_model` | Built-in system — manages resource pack generation and hosting |
| `oraxen` | Delegates skin model lookup entirely to Oraxen |

---

## 🗂️ Adding Custom Skins (Built-in Provider)

Place your skin assets inside `plugins/WeaponSkin/pack/skins/<skin_set_name>/`:

```
pack/skins/my_skin/
├── items.yml
├── models/item/
│   └── my_sword.json
└── textures/item/
    └── my_sword.png
```

**`items.yml` format:**
```yaml
items:
  # Case 1: Model provided (you have a custom JSON model)
  my_sword:
    model: item/my_sword

  # Case 2: Implicit Model Generation (only texture provided)
  my_simple_sword:
    parent_model: item/handheld
    texture: item/my_simple_texture
```

Then register the skin in `skins.yml` and run `/weaponskin pack build` (or `/ws pack build`).

---

## 🔧 Commands

| Command | Description |
|---|---|
| `/weaponskin give <player> <skinId> [amount]` | Give a skin item to a player |
| `/weaponskin giveweapon <player> <skinId> [material] [amount]` | Give a pre-skinned weapon directly (skip drag-and-drop). If `material` is omitted, the skin's first allowed material is used. |
| `/weaponskin remove <player> [amount]` | Give a Skin Remover item to a player |
| `/weaponskin remover <player> [amount]` | Alias for `/weaponskin remove` |
| `/weaponskin list` | List all registered skins |
| `/weaponskin reload` | Reload config, skins, language, and host settings |
| `/weaponskin pack build` | Build the resource pack from skin assets |
| `/weaponskin pack apply <url>` | Apply a built pack hosted on an external URL |

> **Alias:** `/ws` can be used as a shortcut for `/weaponskin` (e.g., `/ws give <player> <skinId>`)

---

## 🔐 Permissions

| Permission | Description | Default |
|---|---|---|
| `weaponskin.admin` | Full access to all commands (parent node) | OP |
| `weaponskin.give` | Give skin items | OP |
| `weaponskin.giveweapon` | Give pre-skinned weapons directly | OP |
| `weaponskin.remove` | Give remover items | OP |
| `weaponskin.list` | List all skins | OP |
| `weaponskin.reload` | Reload configuration | OP |
| `weaponskin.pack` | Build and apply resource packs | OP |

> `weaponskin.admin` grants all child permissions automatically.

---

## 🎮 How It Works (Player Side)

| Action | Result |
|---|---|
| Drag skin item onto a weapon in inventory | Applies the skin |
| Drag Skin Remover onto a skinned weapon | Removes the skin |
| Shift + Right Click a skin item | Preview the skin on your equipped weapon |

---

## 🌐 Resource Pack Hosting

### Self-host (default)
The plugin starts an internal HTTP server on the configured port. Players automatically receive the pack on join.

> Make sure the port is open in your firewall and accessible from the internet.

### External host
Build the pack with `/weaponskin pack build` (or `/ws pack build`), then upload `plugins/WeaponSkin/pack/WeaponSkin-pack.zip` to your CDN or file host. Finally run:
```
/weaponskin pack apply https://your-cdn.com/WeaponSkin-pack.zip
```

---

## 🌍 Multi-Language Support

The plugin supports multiple languages for all user-facing messages.

### Available Languages

| Language | Code | File |
|---|---|---|
| English | `en` | `langs/en.yml` |
| Vietnamese | `vi` | `langs/vi.yml` |

### How to Change Language

1. Open `plugins/WeaponSkin/config.yml`
2. Set `language:` to your desired language code (e.g., `language: vi`)
3. Run `/weaponskin reload` (or `/ws reload`)

### Adding Custom Languages

1. Create a new file in `plugins/WeaponSkin/langs/` (e.g., `fr.yml` for French)
2. Copy the structure from an existing language file
3. Translate all messages
4. Set `language: fr` in `config.yml`

---

## 🛠️ Building from Source

```bash
git clone https://github.com/k4han/WeaponSkin.git
cd WeaponSkin
./gradlew build
```

Output: `build/libs/WeaponSkin-*.jar`

---

## 📄 License

This project is licensed under the MIT License.

---

## ☕ Support

If you find this plugin helpful and want to support its development, consider buying me a coffee!

[![Buy Me A Coffee](https://img.shields.io/badge/Buy_Me_A_Coffee-FFDD00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/kh4n)
