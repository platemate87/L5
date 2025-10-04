using System;
using System.Windows.Forms;
using tool.mapeditor.forms;

namespace tool.mapeditor;

internal static class Program
{
    [STAThread]
    private static void Main()
    {
        Application.SetHighDpiMode(HighDpiMode.PerMonitorV2);
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);
        Application.Run(new MapEditorForm());
    }
}
