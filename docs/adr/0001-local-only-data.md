# Local-only data, no backend or sync

The tracker stores everything on the phone in a local database, with a manual CSV export for backup. There is no server, account system, or cloud sync. The data is sensitive banking activity for a single user on a single device, and a backend would add security and upkeep cost with no benefit. Adding sync later would mean redesigning identity and conflict handling, so this choice is deliberate and not easily undone.
