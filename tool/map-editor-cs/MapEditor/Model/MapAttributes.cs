namespace tool.mapeditor.model;

public class MapAttributes
{
    public bool Underwater { get; set; }
    public bool Markable { get; set; } = true;
    public bool Teleportable { get; set; } = true;
    public bool Escapable { get; set; } = true;
    public bool UseResurrection { get; set; } = true;
    public bool UsePainwand { get; set; } = true;
    public bool EnabledDeathPenalty { get; set; } = true;
    public bool TakePets { get; set; } = true;
    public bool RecallPets { get; set; } = true;
    public bool UsableItem { get; set; } = true;
    public bool UsableSkill { get; set; } = true;

    public MapAttributes Clone()
    {
        return (MapAttributes)MemberwiseClone();
    }
}
