using System;
using System.IO;
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEngine;

public static class BuildScript
{
    private static readonly string[] ScenePaths =
    {
        "Assets/Scenes/MainMenu.unity",
        "Assets/Scenes/Garage.unity",
        "Assets/Scenes/Race_NeonCity.unity"
    };

    public static void BuildAndroid()
    {
        EnsureProjectSetup();
        EditorBuildSettings.scenes = Array.ConvertAll(ScenePaths, path =>
            new EditorBuildSettingsScene(path, true));

        PlayerSettings.productName = "Neon Rush Racing";
        PlayerSettings.applicationIdentifier = "com.neonrush.racing";
        PlayerSettings.bundleVersion = Environment.GetEnvironmentVariable("BUILD_VERSION") ?? "1.0.0";
        PlayerSettings.Android.bundleVersionCode = int.TryParse(
            Environment.GetEnvironmentVariable("BUILD_NUMBER"), out var code) ? code : 1;
        PlayerSettings.SetScriptingBackend(BuildTargetGroup.Android, ScriptingImplementation.IL2CPP);
        PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64 | AndroidArchitecture.ARMv7;
        PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel24;
        PlayerSettings.Android.targetSdkVersion = AndroidSdkVersions.AndroidApiLevel35;
        PlayerSettings.Android.androidIsGame = true;
        PlayerSettings.Android.useCustomKeystore = false;

        EditorUserBuildSettings.buildAppBundle = false;
        EditorUserBuildSettings.androidBuildSystem = AndroidBuildSystem.Gradle;
        EditorUserBuildSettings.androidETC2Fallback = AndroidETC2Fallback.Quality32Bit;

        var output = Environment.GetEnvironmentVariable("BUILD_OUTPUT") ?? "Build/NeonRushRacing-release.apk";
        Directory.CreateDirectory(Path.GetDirectoryName(output) ?? "Build");
        var report = BuildPipeline.BuildPlayer(new BuildPlayerOptions
        {
            scenes = ScenePaths,
            locationPathName = output,
            target = BuildTarget.Android,
            options = BuildOptions.CompressWithLz4HC
        });

        if (report.summary.result != BuildResult.Succeeded)
            throw new Exception($"Android build failed: {report.summary.result}\n{report.summary.totalErrors} errors");

        Debug.Log($"RELEASE APK: {output} ({report.summary.totalSize} bytes)");
    }

    private static void EnsureProjectSetup()
    {
        // Keep every shipped feature and use the project's own one-click setup.
        // It is idempotent and creates only missing generated assets.
        NeonRushSetupWizard.Setup();
        AssetDatabase.SaveAssets();
        AssetDatabase.Refresh();
    }
}
