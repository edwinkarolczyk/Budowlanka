package pl.edwin.budowlanka.data

private const val REGION_2026 = "Małopolskie"

suspend fun AppDao.ensureSeedData() {
    if (getSettings() == null) upsertSettings(AppSettingsEntity())

    val existingWorks = allWorks().associateBy { it.name }.toMutableMap()
    suspend fun ensureWork(
        name: String,
        category: String,
        unit: String,
        rate: Double,
        low: Double = rate,
        high: Double = rate,
        year: Int = 2026,
        source: String,
        includesMaterial: Boolean = false,
        hours: Double = 0.0,
        waste: Double = 10.0
    ): Long {
        val old = existingWorks[name]
        if (old != null) return old.id
        val id = upsertWork(
            WorkEntity(
                name = name,
                category = category,
                unit = unit,
                laborRate = rate,
                laborRateLow = low,
                laborRateHigh = high,
                laborHoursPerUnit = hours,
                defaultWastePct = waste,
                priceRegion = REGION_2026,
                priceYear = year,
                priceSource = source,
                includesMaterial = includesMaterial
            )
        )
        existingWorks[name] = WorkEntity(
            id = id,
            name = name,
            category = category,
            unit = unit,
            laborRate = rate,
            laborRateLow = low,
            laborRateHigh = high,
            laborHoursPerUnit = hours,
            defaultWastePct = waste,
            priceRegion = REGION_2026,
            priceYear = year,
            priceSource = source,
            includesMaterial = includesMaterial
        )
        return id
    }

    val kalkat = "KalKat, Małopolskie, 01.10.2025 — kalkat.pl/stawki-r-g-malopolskie"
    ensureWork("r-g kosztorysowa — instalacje elektryczne", "Stawki r-g kosztorysowe", "rg", 35.83, year = 2025, source = "$kalkat; mediana 35,83; średnia 45,56", hours = 1.0)
    ensureWork("r-g kosztorysowa — instalacje sanitarne", "Stawki r-g kosztorysowe", "rg", 35.99, year = 2025, source = "$kalkat; mediana 35,99; średnia 46,60", hours = 1.0)
    ensureWork("r-g kosztorysowa — roboty inżynieryjne", "Stawki r-g kosztorysowe", "rg", 33.47, year = 2025, source = "$kalkat; mediana 33,47; średnia 43,16", hours = 1.0)
    ensureWork("r-g kosztorysowa — roboty ogólnobudowlane", "Stawki r-g kosztorysowe", "rg", 33.85, year = 2025, source = "$kalkat; mediana 33,85; średnia 43,74", hours = 1.0)
    ensureWork("r-g kosztorysowa — roboty specjalistyczne", "Stawki r-g kosztorysowe", "rg", 41.51, year = 2025, source = "$kalkat; mediana 41,51; średnia 53,18", hours = 1.0)

    val rynek = "Rynek Małopolska 2025/2026 — dane przekazane do katalogu"
    ensureWork("Roboczogodzina murarza / tynkarza", "Stawki godzinowe — rynek", "h", 70.0, 55.0, 85.0, source = rynek, hours = 1.0)
    ensureWork("Roboczogodzina elektryka", "Stawki godzinowe — rynek", "h", 100.0, 70.0, 130.0, source = rynek, hours = 1.0)
    ensureWork("Roboczogodzina hydraulika", "Stawki godzinowe — rynek", "h", 115.0, 80.0, 150.0, source = rynek, hours = 1.0)
    ensureWork("Roboczogodzina dekarza", "Stawki godzinowe — rynek", "h", 87.5, 65.0, 110.0, source = rynek, hours = 1.0)

    val kbEarth = "KB.pl — roboty ziemne, Małopolskie 2026"
    ensureWork("Ręczne wykonanie wykopu 1–1,5 m", "Roboty ziemne", "mb", 179.0, source = kbEarth)
    ensureWork("Ręczne roboty ziemne — wykop do 1 m", "Roboty ziemne", "m³", 89.60, source = kbEarth)
    ensureWork("Koparko-ładowarka z operatorem", "Roboty ziemne", "h", 212.0, source = kbEarth, hours = 1.0)
    ensureWork("Spycharka z operatorem", "Roboty ziemne", "h", 280.0, source = kbEarth, hours = 1.0)

    val kbMasonry = "KB.pl — usługi murarskie, Małopolskie 2026"
    ensureWork("Ściany nośne do 44 cm", "Ściany i murowanie", "m²", 97.0, source = kbMasonry)
    ensureWork("Ściana z pustaków ceramicznych P+W 25", "Ściany i murowanie", "m²", 150.0, source = kbMasonry)
    ensureWork("Ściana z pustaków ceramicznych P+W 11,5", "Ściany i murowanie", "m²", 61.20, source = kbMasonry)
    ensureWork("Ściana z betonu komórkowego 24", "Ściany i murowanie", "m²", 81.40, source = kbMasonry)
    ensureWork("Ściana z betonu komórkowego 12", "Ściany i murowanie", "m²", 48.70, source = kbMasonry)
    ensureWork("Ściany działowe do 15 cm", "Ściany i murowanie", "m²", 66.20, source = kbMasonry)
    ensureWork("Układanie cegły klinkierowej", "Ściany i murowanie", "m²", 167.0, source = kbMasonry)
    ensureWork("Kucie bruzd pod kable w betonie", "Rozbiórki i bruzdy", "mb", 54.30, source = kbMasonry)
    ensureWork("Wykucie otworu w ścianie nośnej", "Rozbiórki i bruzdy", "m²", 967.0, source = kbMasonry)
    ensureWork("Wycinanie otworów w ścianach z cegły", "Rozbiórki i bruzdy", "mb", 335.0, source = kbMasonry)

    val kbHouse = "KB.pl — budowa domów, Małopolskie 2026"
    ensureWork("Budowa ławy fundamentowej", "Fundamenty", "m²", 676.0, source = "$kbHouse; z materiałem", includesMaterial = true)
    ensureWork("Budowa ścian fundamentowych", "Fundamenty", "m²", 88.70, source = kbHouse)
    ensureWork("Izolacja ścian piwnicy", "Izolacje i ocieplenia", "m²", 83.40, source = kbHouse)
    ensureWork("Strop Teriva", "Stropy i żelbet", "m²", 113.0, source = "$kbHouse; robocizna")
    ensureWork("Montaż więźby dachowej", "Dachy", "m²", 72.0, source = kbHouse)
    ensureWork("Montaż membrany dachowej", "Dachy", "m²", 19.90, source = kbHouse)
    ensureWork("Montaż dachówki ceramicznej", "Dachy", "m²", 113.0, source = kbHouse)
    ensureWork("Montaż blachodachówki", "Dachy", "m²", 93.10, source = kbHouse)
    ensureWork("Podbitka dachu", "Dachy", "m²", 86.70, source = kbHouse)
    ensureWork("Ocieplenie styropianem — robocizna", "Izolacje i ocieplenia", "m²", 131.0, source = kbHouse)
    ensureWork("Ocieplenie wełną mineralną — robocizna", "Izolacje i ocieplenia", "m²", 171.0, source = kbHouse)
    ensureWork("Ocieplenie styropianem — robocizna + materiał", "Izolacje i ocieplenia", "m²", 430.0, source = kbHouse, includesMaterial = true)
    ensureWork("Tynk elewacyjny", "Elewacje", "m²", 218.0, source = "$kbHouse; cena wraz z robocizną", includesMaterial = true)
    ensureWork("Tynk cementowo-wapienny / gipsowy", "Tynki i gładzie", "m²", 48.10, source = kbHouse)
    ensureWork("Gładź gipsowa — rynek budowa domu", "Tynki i gładzie", "m²", 49.40, source = kbHouse)
    ensureWork("Posadzka z izolacją", "Posadzki i wylewki", "m²", 108.0, source = kbHouse)
    ensureWork("Posadzka z miksokreta", "Posadzki i wylewki", "m²", 41.50, source = kbHouse)
    ensureWork("Montaż okien", "Stolarka", "mb", 40.80, source = kbHouse)
    ensureWork("Montaż okien połaciowych", "Stolarka", "szt.", 736.0, source = kbHouse)
    ensureWork("Komin wentylacyjny", "Kominy", "szt.", 492.0, source = kbHouse)
    ensureWork("Komin dymowy", "Kominy", "szt.", 5370.0, source = kbHouse)
    ensureWork("Instalacja wod-kan", "Instalacje sanitarne", "punkt", 811.0, source = "$kbHouse; z materiałem", includesMaterial = true)
    ensureWork("Punkt elektryczny — przeróbki", "Instalacje elektryczne", "punkt", 96.70, source = kbHouse)
    ensureWork("Instalacja CO", "Instalacje sanitarne", "punkt", 248.0, source = kbHouse)

    val finish = "Boskie Wykończenia — Małopolska, aktualizacja 2026; widełki rynkowe netto"
    ensureWork("Gruntowanie ścian", "Malowanie", "m²", 11.0, 8.0, 14.0, source = finish)
    ensureWork("Malowanie 2× lateks", "Malowanie", "m²", 28.50, 22.0, 35.0, source = finish)
    ensureWork("Gładź Q2", "Tynki i gładzie", "m²", 35.0, 28.0, 42.0, source = finish)
    ensureWork("Gładź Q3", "Tynki i gładzie", "m²", 55.0, 45.0, 65.0, source = finish)
    ensureWork("Gładź Q4", "Tynki i gładzie", "m²", 105.0, 90.0, 120.0, source = finish)
    ensureWork("Sufit g-k jednowarstwowy Q2", "Płyty GK", "m²", 92.50, 75.0, 110.0, source = finish)
    ensureWork("Sufit g-k dwuwarstwowy", "Płyty GK", "m²", 155.0, 130.0, 180.0, source = finish)
    ensureWork("Flizowanie gres 30×30", "Płytki i okładziny", "m²", 135.0, 110.0, 160.0, source = finish)
    ensureWork("Flizowanie gres rektyfikowany 60×60", "Płytki i okładziny", "m²", 180.0, 140.0, 220.0, source = finish)
    ensureWork("Hydroizolacja podpłytkowa", "Płytki i okładziny", "m²", 57.50, 45.0, 70.0, source = finish)
    ensureWork("Panele laminowane AC4", "Panele i podłogi", "m²", 45.0, 35.0, 55.0, source = finish)
    ensureWork("Panele winylowe SPC", "Panele i podłogi", "m²", 65.0, 50.0, 80.0, source = finish)
    ensureWork("Parkiet dębowy", "Panele i podłogi", "m²", 150.0, 120.0, 180.0, source = finish)
    ensureWork("Ścianka g-k podwójna z wełną", "Płyty GK", "m²", 200.0, 160.0, 240.0, source = finish)
    ensureWork("Punkt elektryczny — cegła", "Instalacje elektryczne", "punkt", 102.50, 75.0, 130.0, source = finish)
    ensureWork("Punkt elektryczny — beton komórkowy", "Instalacje elektryczne", "punkt", 72.50, 55.0, 90.0, source = finish)
    ensureWork("Roboczogodzina fachowca — wykończenia", "Stawki godzinowe — rynek", "h", 87.50, 65.0, 110.0, source = finish, hours = 1.0)

    val oldMaterials = allMaterials().associateBy { it.name }.toMutableMap()
    suspend fun ensureMaterial(
        name: String,
        unit: String,
        budget: Double,
        standard: Double,
        premium: Double
    ): Long {
        val old = oldMaterials[name]
        if (old != null) return old.id
        val id = upsertMaterial(MaterialEntity(name = name, unit = unit, priceBudget = budget, priceStandard = standard, pricePremium = premium))
        oldMaterials[name] = MaterialEntity(id = id, name = name, unit = unit, priceBudget = budget, priceStandard = standard, pricePremium = premium)
        return id
    }

    // Pakiet demonstracyjny zostaje oddzielony od regionalnego cennika — jego normy materiałowe są tylko startowe.
    val styro = ensureWork("Układanie styropianu pod podłogówkę", "Ogrzewanie podłogowe", "m²", 15.0, year = 0, source = "Wartość startowa aplikacji — do ustawienia przez użytkownika", hours = 0.08)
    val foil = ensureWork("Folia / warstwa rozdzielająca", "Ogrzewanie podłogowe", "m²", 5.0, year = 0, source = "Wartość startowa aplikacji — do ustawienia przez użytkownika", hours = 0.03)
    val pipe = ensureWork("Układanie rur podłogówki", "Ogrzewanie podłogowe", "m²", 28.0, year = 0, source = "Wartość startowa aplikacji — do ustawienia przez użytkownika", hours = 0.18)
    val manifold = ensureWork("Montaż i podłączenie rozdzielacza", "Ogrzewanie podłogowe", "kpl.", 750.0, year = 0, source = "Wartość startowa aplikacji — do ustawienia przez użytkownika", hours = 8.0)
    val pressure = ensureWork("Próba szczelności podłogówki", "Ogrzewanie podłogowe", "kpl.", 300.0, year = 0, source = "Wartość startowa aplikacji — do ustawienia przez użytkownika", hours = 4.0)

    val styroMat = ensureMaterial("Styropian podłogowy", "m²", 24.0, 32.0, 42.0)
    val edge = ensureMaterial("Taśma brzegowa", "mb", 1.4, 2.2, 3.2)
    val foilMat = ensureMaterial("Folia budowlana", "m²", 2.0, 3.2, 4.5)
    val pex = ensureMaterial("Rura PEX", "mb", 2.3, 3.1, 4.6)
    val clips = ensureMaterial("Klipsy do rur", "szt.", 0.18, 0.28, 0.38)
    val manifoldMat = ensureMaterial("Rozdzielacz", "kpl.", 700.0, 950.0, 1450.0)

    val existingLinks = allWorkMaterials().toSet()
    suspend fun ensureLink(workId: Long, materialId: Long, qty: Double) {
        val item = WorkMaterialEntity(workId, materialId, qty)
        if (existingLinks.none { it.workId == workId && it.materialId == materialId }) upsertWorkMaterial(item)
    }
    ensureLink(styro, styroMat, 1.0)
    ensureLink(styro, edge, 0.45)
    ensureLink(foil, foilMat, 1.0)
    ensureLink(pipe, pex, 6.5)
    ensureLink(pipe, clips, 18.0)
    ensureLink(manifold, manifoldMat, 1.0)

    val oldTools = allTools().associateBy { it.name }
    suspend fun ensureTool(name: String, code: String): Long =
        oldTools[name]?.id ?: upsertTool(ToolEntity(name = name, code = code))

    val laser = ensureTool("Laser krzyżowy", "LASER")
    val drill = ensureTool("Młotowiertarka", "MLT")
    val pexTool = ensureTool("Zaciskarka PEX", "PEX")
    val pressureTool = ensureTool("Pompa do prób ciśnieniowych", "PROBA")

    val oldToolLinks = allWorkTools()
    suspend fun ensureToolLink(workId: Long, toolId: Long) {
        if (oldToolLinks.none { it.workId == workId && it.toolId == toolId }) upsertWorkTool(WorkToolEntity(workId, toolId))
    }
    ensureToolLink(styro, laser)
    ensureToolLink(styro, drill)
    ensureToolLink(pipe, pexTool)
    ensureToolLink(pressure, pressureTool)

    val oldPackages = allPackages()
    val packageId = oldPackages.firstOrNull { it.name == "Ogrzewanie podłogowe — pełny zakres" }?.id
        ?: upsertPackage(PackageEntity(name = "Ogrzewanie podłogowe — pełny zakres", description = "Pakiet startowy; każdą pozycję można wyłączyć."))
    val existingPackageWorks = allPackageWorks().filter { it.packageId == packageId }
    listOf(styro, foil, pipe, manifold, pressure).forEachIndexed { index, id ->
        if (existingPackageWorks.none { it.workId == id }) upsertPackageWork(PackageWorkEntity(packageId, id, index))
    }

    if (allCrewMembers().isEmpty()) {
        listOf("Osoba 1", "Osoba 2", "Osoba 3").forEach {
            upsertCrewMember(CrewMemberEntity(name = it))
        }
    }
}
