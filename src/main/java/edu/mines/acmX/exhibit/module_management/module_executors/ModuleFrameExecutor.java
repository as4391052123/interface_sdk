package edu.mines.acmX.exhibit.module_management.module_executors;

import edu.mines.acmX.exhibit.module_management.modules.ModuleFrame;
import edu.mines.acmX.exhibit.module_management.modules.ProcessingModule;
import processing.core.PApplet;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Stack;

public class ModuleFrameExecutor extends ModuleExecutor{
    final private Stack<ProcessingModule> moduleStack = new Stack<ProcessingModule>();

    public ModuleFrameExecutor(String fullyQualifiedModuleName, String jarPath) {
        super(fullyQualifiedModuleName, jarPath);
        try{
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        method.invoke(ClassLoader.getSystemClassLoader(), new Object[]{new File(jarPath).toURI().toURL()});
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() throws ModuleRuntimeException {
        try {
            // Freeze active module, if any
            if(!moduleStack.empty()){
                moduleStack.peek().noLoop();
            }
            // Create a new instance of incoming module
            Class modClass = Class.forName(fullyQualifiedModuleName);
            moduleStack.push((ProcessingModule) modClass.newInstance());
            ModuleFrame moduleFrame = new ModuleFrame(moduleStack.peek());
            moduleStack.peek().frame = moduleFrame;
            moduleFrame.addWindowListener(new WindowListener() {
                @Override
                public void windowOpened(WindowEvent e) {
                    // nop?
                }

                @Override
                public void windowClosing(WindowEvent e) {
                    // nop
                }

                @Override
                public void windowClosed(WindowEvent e) {
                    moduleStack.pop();
                    moduleStack.peek().loop();
                }

                @Override
                public void windowIconified(WindowEvent e) {
                    // nop
                }

                @Override
                public void windowDeiconified(WindowEvent e) {
                    // nop
                }

                @Override
                public void windowActivated(WindowEvent e) {
                    // nop
                }

                @Override
                public void windowDeactivated(WindowEvent e) {
                    // nop
                }
            });
            moduleStack.peek().execute();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}