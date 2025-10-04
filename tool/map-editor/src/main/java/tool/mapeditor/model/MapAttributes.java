package tool.mapeditor.model;

/**
 * Container for per-map attribute toggles that are exposed through {@link EditableL1Map}.
 */
public class MapAttributes {
    private boolean underwater;
    private boolean markable;
    private boolean teleportable;
    private boolean escapable;
    private boolean useResurrection;
    private boolean usePainwand;
    private boolean enabledDeathPenalty;
    private boolean takePets;
    private boolean recallPets;
    private boolean usableItem;
    private boolean usableSkill;

    public boolean isUnderwater() {
        return underwater;
    }

    public MapAttributes setUnderwater(boolean underwater) {
        this.underwater = underwater;
        return this;
    }

    public boolean isMarkable() {
        return markable;
    }

    public MapAttributes setMarkable(boolean markable) {
        this.markable = markable;
        return this;
    }

    public boolean isTeleportable() {
        return teleportable;
    }

    public MapAttributes setTeleportable(boolean teleportable) {
        this.teleportable = teleportable;
        return this;
    }

    public boolean isEscapable() {
        return escapable;
    }

    public MapAttributes setEscapable(boolean escapable) {
        this.escapable = escapable;
        return this;
    }

    public boolean isUseResurrection() {
        return useResurrection;
    }

    public MapAttributes setUseResurrection(boolean useResurrection) {
        this.useResurrection = useResurrection;
        return this;
    }

    public boolean isUsePainwand() {
        return usePainwand;
    }

    public MapAttributes setUsePainwand(boolean usePainwand) {
        this.usePainwand = usePainwand;
        return this;
    }

    public boolean isEnabledDeathPenalty() {
        return enabledDeathPenalty;
    }

    public MapAttributes setEnabledDeathPenalty(boolean enabledDeathPenalty) {
        this.enabledDeathPenalty = enabledDeathPenalty;
        return this;
    }

    public boolean isTakePets() {
        return takePets;
    }

    public MapAttributes setTakePets(boolean takePets) {
        this.takePets = takePets;
        return this;
    }

    public boolean isRecallPets() {
        return recallPets;
    }

    public MapAttributes setRecallPets(boolean recallPets) {
        this.recallPets = recallPets;
        return this;
    }

    public boolean isUsableItem() {
        return usableItem;
    }

    public MapAttributes setUsableItem(boolean usableItem) {
        this.usableItem = usableItem;
        return this;
    }

    public boolean isUsableSkill() {
        return usableSkill;
    }

    public MapAttributes setUsableSkill(boolean usableSkill) {
        this.usableSkill = usableSkill;
        return this;
    }
}
