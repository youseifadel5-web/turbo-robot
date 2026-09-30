using UnityEngine;
using UnityEngine.UI;

namespace NeonRush.UI
{
    public static class UiTheme
    {
        public static readonly Color Bg = new Color(0.02f, 0.027f, 0.047f, 0.88f);
        public static readonly Color Panel = new Color(0.047f, 0.063f, 0.094f, 0.82f);
        public static readonly Color Cyan = new Color(0f, 0.94f, 1f, 1f);
        public static readonly Color Magenta = new Color(1f, 0.17f, 0.84f, 1f);
        public static readonly Color Text = new Color(0.95f, 0.96f, 1f, 1f);
        public static readonly Color Muted = new Color(0.6f, 0.64f, 0.71f, 1f);
        public static readonly Color Gold = new Color(1f, 0.78f, 0.3f, 1f);

        public static Font Font => Resources.GetBuiltinResource<Font>("Arial.ttf");

        public static Image PanelBox(Transform parent, string name, Vector2 anchor, Vector2 pos, Vector2 size, Color? color = null, Color? border = null)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            var rt = go.AddComponent<RectTransform>();
            rt.anchorMin = anchor;
            rt.anchorMax = anchor;
            rt.pivot = new Vector2(anchor.x, anchor.y);
            rt.anchoredPosition = pos;
            rt.sizeDelta = size;
            var img = go.AddComponent<Image>();
            img.color = color ?? Panel;
            if (border.HasValue)
            {
                var outline = go.AddComponent<Outline>();
                outline.effectColor = border.Value;
                outline.effectDistance = new Vector2(1.5f, -1.5f);
            }
            return img;
        }

        public static Text Label(Transform parent, string name, string value, Vector2 anchor, Vector2 pos, Vector2 size, int fontSize, Color color, TextAnchor align = TextAnchor.MiddleLeft)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            var rt = go.AddComponent<RectTransform>();
            rt.anchorMin = anchor;
            rt.anchorMax = anchor;
            rt.pivot = new Vector2(anchor.x, anchor.y);
            rt.anchoredPosition = pos;
            rt.sizeDelta = size;
            var t = go.AddComponent<Text>();
            t.text = value;
            t.font = Font;
            t.fontSize = fontSize;
            t.color = color;
            t.alignment = align;
            t.horizontalOverflow = HorizontalWrapMode.Overflow;
            t.verticalOverflow = VerticalWrapMode.Overflow;
            t.raycastTarget = false;
            return t;
        }
    }
}
