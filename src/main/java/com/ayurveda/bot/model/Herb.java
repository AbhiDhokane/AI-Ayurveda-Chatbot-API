package com.ayurveda.bot.model;

import java.util.List;

public class Herb {
    private String id;
    private String name;
    private String sanskritName;
    private String botanicalName;
    private String family;
    private String englishName;
    private List<String> commonNames;
    private List<String> partsUsed;
    private String rasa;          // Taste
    private String guna;          // Qualities
    private String virya;         // Potency (Hot/Cold)
    private String vipaka;        // Post-digestive effect
    private String doshaEffect;   // Effect on Vata, Pitta, Kapha
    private String prabhava;      // Special effect
    private String description;
    private List<String> benefits;
    private List<String> traditionalUses;
    private List<String> modernApplications;
    private String dosage;
    private String precautions;
    private String howToUse;
    private List<String> formulations;
    private String category;      // Rasayana, Digestive, etc.

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSanskritName() { return sanskritName; }
    public void setSanskritName(String sanskritName) { this.sanskritName = sanskritName; }

    public String getBotanicalName() { return botanicalName; }
    public void setBotanicalName(String botanicalName) { this.botanicalName = botanicalName; }

    public String getFamily() { return family; }
    public void setFamily(String family) { this.family = family; }

    public String getEnglishName() { return englishName; }
    public void setEnglishName(String englishName) { this.englishName = englishName; }

    public List<String> getCommonNames() { return commonNames; }
    public void setCommonNames(List<String> commonNames) { this.commonNames = commonNames; }

    public List<String> getPartsUsed() { return partsUsed; }
    public void setPartsUsed(List<String> partsUsed) { this.partsUsed = partsUsed; }

    public String getRasa() { return rasa; }
    public void setRasa(String rasa) { this.rasa = rasa; }

    public String getGuna() { return guna; }
    public void setGuna(String guna) { this.guna = guna; }

    public String getVirya() { return virya; }
    public void setVirya(String virya) { this.virya = virya; }

    public String getVipaka() { return vipaka; }
    public void setVipaka(String vipaka) { this.vipaka = vipaka; }

    public String getDoshaEffect() { return doshaEffect; }
    public void setDoshaEffect(String doshaEffect) { this.doshaEffect = doshaEffect; }

    public String getPrabhava() { return prabhava; }
    public void setPrabhava(String prabhava) { this.prabhava = prabhava; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits) { this.benefits = benefits; }

    public List<String> getTraditionalUses() { return traditionalUses; }
    public void setTraditionalUses(List<String> traditionalUses) { this.traditionalUses = traditionalUses; }

    public List<String> getModernApplications() { return modernApplications; }
    public void setModernApplications(List<String> modernApplications) { this.modernApplications = modernApplications; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getPrecautions() { return precautions; }
    public void setPrecautions(String precautions) { this.precautions = precautions; }

    public String getHowToUse() { return howToUse; }
    public void setHowToUse(String howToUse) { this.howToUse = howToUse; }

    public List<String> getFormulations() { return formulations; }
    public void setFormulations(List<String> formulations) { this.formulations = formulations; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
