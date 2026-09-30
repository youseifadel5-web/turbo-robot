using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;
using UnityEngine.SceneManagement;
using System.IO;
using System.Linq;
using NeonRush.Tracks;

/// <summary>
/// NEON RUSH RACING v0.9 one-click project setup.
/// Menu: NeonRush > Setup Project.
/// - URP pipeline asset + renderer + global volume (bloom per URP_Look_Settings)
/// - Skybox material from the generated night-city panorama
/// - Car prefabs from Assets/Art/Cars OBJs (materials, LODs, colliders) -> Resources
/// - Traffic prefabs
/// - TrackConfig + CarStats assets
/// - Scenes: MainMenu, Garage, Race_NeonCity (+ Build Settings)
/// - Android Player Settings (IL2CPP/ARM64, orientation, identifier)
/// </summary>
public static class NeonRushSetupWizard
{
    static readonly string[] CarIds = { "falcon_s", "vortex_gt", "titan_x", "aurora_r", "nomad_x" };
    static readonly string[] RivalTraffic = { "traffic_sedan", "traffic_van" };

    [MenuItem("NeonRush/Setup Project (one click)")]
    public static void Setup()
    {
        try
        {
            AssetDatabase.StartAssetEditing();
            EnsureFolders();
            CreateUrpAssets();
            CreateSkybox();
            CreateMaterials();
            CreateCarPrefabs();
            CreateTrafficPrefabs();
            CreateDataAssets();
        }
        finally
        {
            AssetDatabase.StopAssetEditing();
        }

        AssetDatabase.SaveAssets();
        AssetDatabase.Refresh();
        CreateScenes();
        ConfigureAndroid();
        Debug.Log("[NeonRush Setup] DONE. Open MainMenu.unity and press Play. " +
                  "Build: File > Build Settings > Android > Build (APK).");
    }

    // ------------------------------------------------------------------ folders
    static void EnsureFolders()
    {
        foreach (var f in new[] { "Assets/Settings", "Assets/Materials", "Assets/Prefabs",
            "Assets/Resources/Prefabs", "Assets/Data", "Assets/Shaders", "Assets/Scenes" })
            if (!AssetDatabase.IsValidFolder(f))
                AssetDatabase.CreateFolder(Path.GetDirectoryName(f).Replace('\\', '/'), Path.GetFileName(f));
    }

    // ------------------------------------------------------------------ URP
    static void CreateUrpAssets()
    {
        if (GraphicsSettings.defaultRenderPipeline != null) return; // already configured

        var rendererData = ScriptableObject.CreateInstance<UniversalRendererData>();
        var ppData = AssetDatabase.LoadAssetAtPath<PostProcessData>(
            "Packages/com.unity.render-pipelines.universal/Runtime/Data/PostProcessData.asset");
        if (ppData != null)
        {
            var soR = new SerializedObject(rendererData);
            var ppProp = soR.FindProperty("m_PostProcessData");
            if (ppProp != null) { ppProp.objectReferenceValue = ppData; soR.ApplyModifiedPropertiesWithoutUndo(); }
        }

        var rendererPath = "Assets/Settings/NeonRushRenderer.asset";
        AssetDatabase.CreateAsset(rendererData, rendererPath);

        var pipeline = ScriptableObject.CreateInstance<UniversalRenderPipelineAsset>();
        var so = new SerializedObject(pipeline);
        var list = so.FindProperty("m_RendererDataList");
        if (list != null)
        {
            list.arraySize = 1;
            list.GetArrayElementAtIndex(0).objectReferenceValue = rendererData;
            var idx = so.FindProperty("m_DefaultRendererIndex");
            if (idx != null) idx.intValue = 0;
        }
        so.ApplyModifiedPropertiesWithoutUndo();

        // mobile-friendly defaults
        pipeline.supportsHDR = false;
        pipeline.msaaSampleCount = MsaaQuality.Mode2x;
        pipeline.shadowDistance = 60f;
        pipeline.renderScale = 1f;

        var pipelinePath = "Assets/Settings/NeonRushURP.asset";
        AssetDatabase.CreateAsset(pipeline, pipelinePath);
        AssetDatabase.SaveAssets();
        GraphicsSettings.defaultRenderPipeline = pipeline;
        QualitySettings.renderPipeline = pipeline;
        Debug.Log("[NeonRush Setup] URP pipeline created and assigned: " + pipelinePath);
    }

    // ------------------------------------------------------------------ skybox
    static void CreateSkybox()
    {
        const string path = "Assets/Art/Skybox/SkyNightCity.mat";
        if (File.Exists(path)) return;
        var shader = Shader.Find("Skybox/Panoramic");
        if (shader == null) { Debug.LogWarning("[NeonRush Setup] Skybox/Panoramic shader missing — skipped."); return; }
        var tex = AssetDatabase.LoadAssetAtPath<Texture2D>("Assets/Art/Skybox/sky_night_city_1k.png");
        var mat = new Material(shader) { name = "SkyNightCity" };
        if (tex != null) { mat.mainTexture = tex; mat.SetFloat("_Mapping", 1f); }
        AssetDatabase.CreateAsset(mat, path);
    }

    // ------------------------------------------------------------------ materials
    static Material _body, _glass, _tires, _rims, _lightsF, _lightsR, _glow;

    static void CreateMaterials()
    {
        _body = LitMat("Car_Body", "#0A0C10", metallic: 0.78f, smooth: 0.55f);
        _glass = LitMat("Car_Glass", "#05070C", metallic: 0.4f, smooth: 0.95f);
        _tires = LitMat("Car_Tires", "#0D0D0F", metallic: 0f, smooth: 0.2f);
        _rims = LitMat("Car_Rims", "#9AA0AA", metallic: 0.9f, smooth: 0.7f);
        _lightsF = LitMat("Car_LightsF", "#0A2A33", metallic: 0f, smooth: 0.5f);
        EnableEmission(_lightsF, new Color(0f, 0.94f, 1f) * 3.5f);
        _lightsR = LitMat("Car_LightsR", "#2A0716", metallic: 0f, smooth: 0.5f);
        EnableEmission(_lightsR, new Color(1f, 0.17f, 0.84f) * 3.5f);

        var glowShader = Shader.Find("NeonRush/AdditiveUnlit");
        if (glowShader != null)
        {
            var tex = AssetDatabase.LoadAssetAtPath<Texture2D>("Assets/Art/VFX/tex_glow.png");
            _glow = new Material(glowShader) { name = "Car_Glow", mainTexture = tex };
            _glow.SetColor("_BaseColor", new Color(0f, 0.94f, 1f, 0.85f));
            _glow.SetFloat("_Intensity", 1.4f);
            SaveMat(_glow, "Assets/Materials/Car_Glow.mat");
        }
    }

    static Material LitMat(string name, string hex, float metallic, float smooth)
    {
        var path = "Assets/Materials/" + name + ".mat";
        var existing = AssetDatabase.LoadAssetAtPath<Material>(path);
        if (existing != null) return existing;

        var shader = Shader.Find("Universal Render Pipeline/Lit");
        var mat = new Material(shader) { name = name };
        ColorUtility.TryParseHtmlString(hex, out var c);
        mat.color = c;
        if (mat.HasProperty("_Metallic")) mat.SetFloat("_Metallic", metallic);
        if (mat.HasProperty("_Smoothness")) mat.SetFloat("_Smoothness", smooth);
        SaveMat(mat, path);
        return mat;
    }

    static void EnableEmission(Material mat, Color emission)
    {
        mat.EnableKeyword("_EMISSION");
        if (mat.HasProperty("_EmissionColor")) mat.SetColor("_EmissionColor", emission);
        EditorUtility.SetDirty(mat);
    }

    static void SaveMat(Material mat, string path)
    {
        AssetDatabase.CreateAsset(mat, path);
    }

    // ------------------------------------------------------------------ prefabs
    static void CreateCarPrefabs()
    {
        var mats = new[] { _body, _glass, _lightsF, _lightsR, _glow, _rims, _tires };

        foreach (var carId in CarIds)
        {
            var objPath = $"Assets/Art/Cars/{carId}.obj";
            var lodPath = $"Assets/Art/Cars/{carId}_lod1.obj";
            var root = BuildCarRoot(carId, objPath, lodPath, mats);
            if (root == null) continue;
            PrefabUtility.SaveAsPrefabAsset(root, $"Assets/Resources/Prefabs/Car_{carId}.prefab");
            Object.DestroyImmediate(root);
            Debug.Log("[NeonRush Setup] prefab: Car_" + carId);
        }
    }

    static void CreateTrafficPrefabs()
    {
        var mats = new[] { _body, _glass, _lightsF, _lightsR, _rims, _tires };
        foreach (var id in RivalTraffic)
        {
            var objPath = $"Assets/Art/Cars/{id}.obj";
            var root = BuildCarRoot(id, objPath, null, mats);
            if (root == null) continue;
            PrefabUtility.SaveAsPrefabAsset(root, $"Assets/Resources/Prefabs/{(id == "traffic_sedan" ? "Traffic_Sedan" : "Traffic_Van")}.prefab");
            Object.DestroyImmediate(root);
            Debug.Log("[NeonRush Setup] prefab: " + id);
        }
    }

    static GameObject BuildCarRoot(string name, string objPath, string lodPath, Material[] mats)
    {
        AssetDatabase.ImportAsset(objPath, ImportAssetOptions.ForceUpdate);
        var model = AssetDatabase.LoadAssetAtPath<GameObject>(objPath);
        if (model == null)
        {
            Debug.LogWarning("[NeonRush Setup] missing model: " + objPath);
            return null;
        }

        var root = new GameObject("CarRoot_" + name);

        // LOD0
        var lod0Go = Object.Instantiate(model, root.transform);
        lod0Go.name = "LOD0";
        SetupRenderer(lod0Go, mats);

        // LOD1
        if (lodPath != null && File.Exists(lodPath))
        {
            AssetDatabase.ImportAsset(lodPath, ImportAssetOptions.ForceUpdate);
            var lodModel = AssetDatabase.LoadAssetAtPath<GameObject>(lodPath);
            if (lodModel != null)
            {
                var lod1Go = Object.Instantiate(lodModel, root.transform);
                lod1Go.name = "LOD1";
                var lodMats = mats.Where(m => m != _lightsF && m != _lightsR).ToArray();
                SetupRenderer(lod1Go, lodMats);
            }
        }

        // LODGroup
        var renderers = root.GetComponentsInChildren<MeshRenderer>();
        if (renderers.Length >= 2)
        {
            var lodg = root.AddComponent<LODGroup>();
            lodg.SetLODs(new[]
            {
                new LOD(0.55f, new[] { renderers[0] }),
                new LOD(0.12f, new[] { renderers[1] })
            });
            lodg.RecalculateBounds();
        }
        else if (renderers.Length == 1)
        {
            renderers[0].gameObject.AddComponent<LODGroup>();
        }

        return root;
    }

    static void SetupRenderer(GameObject go, Material[] mats)
    {
        foreach (var r in go.GetComponentsInChildren<MeshRenderer>())
        {
            r.sharedMaterials = mats;
            r.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.On;
            r.receiveShadows = true;
        }
        // strip auto-generated colliders from OBJ import (root adds its own)
        foreach (var col in go.GetComponentsInChildren<Collider>())
            Object.DestroyImmediate(col);
    }

    // ------------------------------------------------------------------ data
    static void CreateDataAssets()
    {
        // TrackConfig
        var cfgPath = "Assets/Data/Track_NeonCity_Medium.asset";
        var cfg = AssetDatabase.LoadAssetAtPath<TrackConfig>(cfgPath);
        if (cfg == null)
        {
            cfg = ScriptableObject.CreateInstance<TrackConfig>();
            AssetDatabase.CreateAsset(cfg, cfgPath);
        }
        cfg.trackId = "neon_city_01";
        cfg.displayName = "Neon City Circuit";
        cfg.lengthPreset = TrackLengthPreset.Medium;
        cfg.laps = 2;
        cfg.hasTraffic = true;
        cfg.trafficDensity = 1;
        EditorUtility.SetDirty(cfg);

        // CarStats assets (data-driven, mirrors CarCatalog)
        foreach (var def in NeonRush.Cars.CarCatalog.All)
        {
            var p = "Assets/Data/Stats_" + def.carId + ".asset";
            var stats = AssetDatabase.LoadAssetAtPath<NeonRush.Cars.CarStats>(p);
            if (stats == null)
            {
                stats = ScriptableObject.CreateInstance<NeonRush.Cars.CarStats>();
                AssetDatabase.CreateAsset(stats, p);
            }
            var runtime = NeonRush.Cars.CarCatalog.CreateStats(def.carId);
            EditorUtility.CopySerialized(runtime, stats);
            stats.name = "Stats_" + def.carId;
            EditorUtility.SetDirty(stats);
        }
    }

    // ------------------------------------------------------------------ scenes
    static void CreateScenes()
    {
        var cfg = AssetDatabase.LoadAssetAtPath<TrackConfig>("Assets/Data/Track_NeonCity_Medium.asset");
        var skyMat = AssetDatabase.LoadAssetAtPath<Material>("Assets/Art/Skybox/SkyNightCity.mat");

        // MainMenu (shipped scenes are kept — v0.10 authors them in-repo)
        if (!File.Exists("Assets/Scenes/MainMenu.unity"))
            NewSceneWithAssembler("MainMenu", startRace: false, cfg, skyMat);

        // Race
        if (!File.Exists("Assets/Scenes/Race_NeonCity.unity"))
            NewSceneWithAssembler("Race_NeonCity", startRace: true, cfg, skyMat);

        // Garage
        if (!File.Exists("Assets/Scenes/Garage.unity"))
        {
            var garageScene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
            var bootGo = new GameObject("GarageBoot");
            bootGo.AddComponent<NeonRush.UI.GarageSceneBoot>();
            if (skyMat != null) RenderSettings.skybox = skyMat;
            EditorSceneManager.SaveScene(garageScene, "Assets/Scenes/Garage.unity");
        }

        // reopen menu scene for the editor
        if (File.Exists("Assets/Scenes/MainMenu.unity"))
            EditorSceneManager.OpenScene("Assets/Scenes/MainMenu.unity");

        // Build settings
        var scenes = new[]
        {
            new EditorBuildSettingsScene("Assets/Scenes/MainMenu.unity", true),
            new EditorBuildSettingsScene("Assets/Scenes/Garage.unity", true),
            new EditorBuildSettingsScene("Assets/Scenes/Race_NeonCity.unity", true)
        };
        EditorBuildSettings.scenes = scenes;
        Debug.Log("[NeonRush Setup] scenes created + added to Build Settings.");
    }

    static Scene NewSceneWithAssembler(string name, bool startRace, TrackConfig cfg, Material skyMat)
    {
        var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
        var assemblerGo = new GameObject("SceneAssembler");
        var asm = assemblerGo.AddComponent<NeonRush.Environment.SceneAssembler>();
        asm.buildOnStart = true;
        asm.startRace = startRace;
        asm.trackConfig = cfg;
        if (skyMat != null) RenderSettings.skybox = skyMat;
        EditorSceneManager.SaveScene(scene, "Assets/Scenes/" + name + ".unity");
        return scene;
    }

    // ------------------------------------------------------------------ android
    static void ConfigureAndroid()
    {
        PlayerSettings.companyName = "NeonRushStudio";
        PlayerSettings.productName = "NeonRushRacing";
        PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android, "com.neonrushstudio.racing");
        PlayerSettings.defaultInterfaceOrientation = UIOrientation.LandscapeLeft;
        PlayerSettings.allowedAutorotateToLandscapeLeft = true;
        PlayerSettings.allowedAutorotateToLandscapeRight = true;
        PlayerSettings.allowedAutorotateToPortrait = false;
        PlayerSettings.allowedAutorotateToPortraitUpsideDown = false;

        PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel24;
        PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64;
        PlayerSettings.SetScriptingBackend(BuildTargetGroup.Android, ScriptingImplementation.IL2CPP);
        PlayerSettings.Android.forceInternetPermission = false;
        PlayerSettings.stripEngineCode = true;
        PlayerSettings.SetIl2CppCompilerConfiguration(BuildTargetGroup.Android, Il2CppCompilerConfiguration.Release);

        Debug.Log("[NeonRush Setup] Android Player Settings configured (IL2CPP/ARM64, API 24+).");
    }

    [MenuItem("NeonRush/Add READ_MEDIA_AUDIO to Android manifest (Android 13+ music)")]
    public static void AddMusicPermissionManifest()
    {
        // Unity does not expose arbitrary permissions via PlayerSettings API;
        // a small custom main manifest is the supported route. Unity merges it.
        const string manifest =
@"<?xml version=""1.0"" encoding=""utf-8""?>
<manifest xmlns:android=""http://schemas.android.com/apk/res/android"">
  <uses-permission android:name=""android.permission.READ_MEDIA_AUDIO"" />
  <uses-permission android:name=""android.permission.READ_EXTERNAL_STORAGE"" android:maxSdkVersion=""32"" />
  <application>
    <activity android:name=""com.unity3d.player.UnityPlayerActivity"" android:theme=""@style/UnityThemeSelector"">
      <intent-filter>
        <action android:name=""android.intent.action.MAIN"" />
        <category android:name=""android.intent.category.LAUNCHER"" />
      </intent-filter>
      <meta-data android:name=""unityplayer.UnityActivity"" android:value=""true"" />
    </activity>
  </application>
</manifest>
";
        var dir = "Assets/Plugins/Android";
        if (!AssetDatabase.IsValidFolder(dir))
            AssetDatabase.CreateFolder("Assets/Plugins", "Android");
        var path = dir + "/AndroidManifest.xml";
        if (File.Exists(path))
        {
            Debug.LogWarning("[NeonRush Setup] custom AndroidManifest.xml already exists — not overwritten.");
            return;
        }
        File.WriteAllText(path, manifest);
        AssetDatabase.ImportAsset(path);
        Debug.Log("[NeonRush Setup] AndroidManifest.xml written with READ_MEDIA_AUDIO permission.");
    }
}
