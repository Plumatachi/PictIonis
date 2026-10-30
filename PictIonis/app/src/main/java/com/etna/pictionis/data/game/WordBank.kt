package com.etna.pictionis.data.game

// Liste de mots de base, classés du plus simple au plus difficile à dessiner.
object WordBank {

    val easy = listOf(
        "chat", "chien", "maison", "soleil", "arbre", "pomme", "voiture", "fleur",
        "poisson", "bateau", "lune", "étoile", "ballon", "gâteau", "livre", "chapeau",
        "lunettes", "vélo", "banane", "nuage", "oiseau", "téléphone", "clé", "parapluie",
        "glace", "train", "avion", "cœur", "main", "pied", "œil", "bouche",
        "lit", "chaise", "table", "porte", "fenêtre", "verre", "cuillère", "fourchette",
        "tasse", "bougie", "cadeau", "sapin", "neige", "pluie", "montagne", "plage",
        "serpent", "escargot", "tortue", "lapin", "souris", "cochon", "vache", "canard",
        "abeille", "papillon", "fraise", "carotte"
    )

    val medium = listOf(
        "arc-en-ciel", "château", "dragon", "pirate", "fusée", "robot", "girafe", "éléphant",
        "pingouin", "requin", "pieuvre", "volcan", "phare", "moulin", "cactus", "champignon",
        "citrouille", "fantôme", "sorcière", "couronne", "épée", "bouclier", "guitare", "piano",
        "trompette", "tambour", "horloge", "boussole", "loupe", "microscope", "télescope", "ampoule",
        "aimant", "parachute", "hélicoptère", "sous-marin", "tracteur", "ambulance", "pompier", "astronaute",
        "cowboy", "momie", "licorne", "sirène", "kangourou", "hérisson", "chauve-souris", "araignée",
        "dinosaure", "igloo", "tente", "pyramide", "statue", "pont", "tunnel", "ascenseur",
        "escalier", "toboggan", "balançoire", "trampoline"
    )

    val hard = listOf(
        "électricité", "gravité", "écho", "vertige", "nostalgie", "liberté", "photosynthèse", "évolution",
        "silence", "rêve", "cauchemar", "jalousie", "embouteillage", "déménagement", "anniversaire", "camouflage",
        "équilibre", "reflet", "ombre", "mirage", "tempête", "éclipse", "marée", "boomerang",
        "labyrinthe", "hypnose", "magnétisme", "recyclage", "vaccin", "satellite", "algorithme", "hiéroglyphe",
        "trou noir", "big bang", "aurore boréale", "tremblement de terre", "effet papillon", "jeu de mots",
        "coup de foudre", "chasse au trésor"
    )

    val all: List<String> get() = easy + medium + hard

    fun forDifficulty(difficulty: Difficulty): List<String> = when (difficulty) {
        Difficulty.EASY -> easy
        Difficulty.MEDIUM -> medium
        Difficulty.HARD -> hard
        Difficulty.MIXED -> all
    }
}
