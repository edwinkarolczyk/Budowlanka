package pl.edwin.budowlanka.data

suspend fun AppDao.ensureSeedData() {
    if (getSettings() == null) upsertSettings(AppSettingsEntity())
    if (countWorks() > 0) return

    val styro = upsertWork(WorkEntity(name="Układanie styropianu", category="Ogrzewanie podłogowe", unit="m²", laborRate=15.0, laborHoursPerUnit=0.08))
    val foil = upsertWork(WorkEntity(name="Folia / warstwa rozdzielająca", category="Ogrzewanie podłogowe", unit="m²", laborRate=5.0, laborHoursPerUnit=0.03))
    val pipe = upsertWork(WorkEntity(name="Układanie rur podłogówki", category="Ogrzewanie podłogowe", unit="m²", laborRate=28.0, laborHoursPerUnit=0.18))
    val manifold = upsertWork(WorkEntity(name="Montaż i podłączenie rozdzielacza", category="Ogrzewanie podłogowe", unit="kpl.", laborRate=750.0, laborHoursPerUnit=8.0))
    val pressure = upsertWork(WorkEntity(name="Próba szczelności", category="Ogrzewanie podłogowe", unit="kpl.", laborRate=300.0, laborHoursPerUnit=4.0))
    val screed = upsertWork(WorkEntity(name="Wylewka", category="Posadzki", unit="m²", laborRate=24.0, laborHoursPerUnit=0.13))
    val paint = upsertWork(WorkEntity(name="Malowanie ścian", category="Wykończenie", unit="m²", laborRate=18.0, laborHoursPerUnit=0.08))
    val smooth = upsertWork(WorkEntity(name="Gładź", category="Wykończenie", unit="m²", laborRate=35.0, laborHoursPerUnit=0.18))

    val styroMat = upsertMaterial(MaterialEntity(name="Styropian podłogowy", unit="m²", priceBudget=24.0, priceStandard=32.0, pricePremium=42.0))
    val edge = upsertMaterial(MaterialEntity(name="Taśma brzegowa", unit="mb", priceBudget=1.4, priceStandard=2.2, pricePremium=3.2))
    val foilMat = upsertMaterial(MaterialEntity(name="Folia budowlana", unit="m²", priceBudget=2.0, priceStandard=3.2, pricePremium=4.5))
    val pex = upsertMaterial(MaterialEntity(name="Rura PEX", unit="mb", priceBudget=2.3, priceStandard=3.1, pricePremium=4.6))
    val clips = upsertMaterial(MaterialEntity(name="Klipsy do rur", unit="szt.", priceBudget=0.18, priceStandard=0.28, pricePremium=0.38))
    val manifoldMat = upsertMaterial(MaterialEntity(name="Rozdzielacz", unit="kpl.", priceBudget=700.0, priceStandard=950.0, pricePremium=1450.0))
    val paintMat = upsertMaterial(MaterialEntity(name="Farba", unit="l", priceBudget=12.0, priceStandard=22.0, pricePremium=36.0))

    upsertWorkMaterial(WorkMaterialEntity(styro, styroMat, 1.0))
    upsertWorkMaterial(WorkMaterialEntity(styro, edge, 0.45))
    upsertWorkMaterial(WorkMaterialEntity(foil, foilMat, 1.0))
    upsertWorkMaterial(WorkMaterialEntity(pipe, pex, 6.5))
    upsertWorkMaterial(WorkMaterialEntity(pipe, clips, 18.0))
    upsertWorkMaterial(WorkMaterialEntity(manifold, manifoldMat, 1.0))
    upsertWorkMaterial(WorkMaterialEntity(paint, paintMat, 0.2))

    val laser = upsertTool(ToolEntity(name="Laser krzyżowy", code="LASER"))
    val drill = upsertTool(ToolEntity(name="Młotowiertarka", code="MLT"))
    val pexTool = upsertTool(ToolEntity(name="Zaciskarka PEX", code="PEX"))
    val pressureTool = upsertTool(ToolEntity(name="Pompa do prób ciśnieniowych", code="PROBA"))
    val roller = upsertTool(ToolEntity(name="Wałek malarski", code="WAL"))

    upsertWorkTool(WorkToolEntity(styro, laser))
    upsertWorkTool(WorkToolEntity(styro, drill))
    upsertWorkTool(WorkToolEntity(pipe, pexTool))
    upsertWorkTool(WorkToolEntity(pressure, pressureTool))
    upsertWorkTool(WorkToolEntity(paint, roller))

    val pkg = upsertPackage(PackageEntity(name="Ogrzewanie podłogowe — pełny zakres", description="Pakiet startowy, każdą pozycję można usunąć z wyceny."))
    listOf(styro, foil, pipe, manifold, pressure, screed).forEachIndexed { index, id ->
        upsertPackageWork(PackageWorkEntity(pkg, id, index))
    }

    listOf("Osoba 1", "Osoba 2", "Osoba 3").forEach {
        upsertCrewMember(CrewMemberEntity(name=it))
    }
}
