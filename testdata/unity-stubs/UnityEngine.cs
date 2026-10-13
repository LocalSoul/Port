// Stubs der Unity-API für Frontend-Fixtures (M2.1). Nur Signaturen, Rümpfe werfen.
// Namen und Signaturen folgen der öffentlichen Unity-Dokumentation.
using System;

namespace UnityEngine
{
    public class Object
    {
    }

    public class Component : Object
    {
        public Transform transform => throw null;
    }

    public class Behaviour : Component
    {
    }

    public class MonoBehaviour : Behaviour
    {
    }

    public class Transform : Component
    {
        public Vector3 position { get => throw null; set => throw null; }

        public void Translate(Vector3 translation) => throw null;
    }

    public struct Vector3
    {
        public float x;
        public float y;
        public float z;

        public Vector3(float x, float y, float z) => throw null;

        public static Vector3 operator *(Vector3 a, float d) => throw null;

        public static Vector3 operator +(Vector3 a, Vector3 b) => throw null;
    }

    public static class Input
    {
        public static float GetAxis(string axisName) => throw null;

        public static bool GetKey(string name) => throw null;
    }

    public static class Time
    {
        public static float deltaTime => throw null;
    }

    public static class Mathf
    {
        public static float Abs(float f) => throw null;

        public static int Clamp(int value, int min, int max) => throw null;
    }

    public static class Debug
    {
        public static void Log(object message) => throw null;
    }

    [AttributeUsage(AttributeTargets.Field)]
    public sealed class SerializeField : Attribute
    {
    }
}
