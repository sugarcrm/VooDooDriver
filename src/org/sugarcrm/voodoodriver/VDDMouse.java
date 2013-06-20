/*
Copyright 2011-2012 SugarCRM Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
Please see the License for the specific language governing permissions and
limitations under the License.
*/

package org.sugarcrm.voodoodriver;

import java.awt.Robot;
import java.awt.event.InputEvent;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.internal.Locatable;

public class VDDMouse {

   private Reporter reporter = null;
   private Robot robo = null;

   public VDDMouse(Reporter reporter) {
      this.reporter = reporter;

      try {
         this.robo = new Robot();
      } catch (Exception exp) {
         this.reporter.ReportException(exp);
         this.robo = null;
      }
   }

   public void DnD(WebElement wsrc, WebElement wdst) {
      int src_x, src_y, dst_x, dst_y;
      Dimension srcDim = null;
      Dimension dstDim = null;
      RemoteWebElement src, dst;

      if (this.robo == null) {
         return;
      }

      this.reporter.Log("Starting DnD.");

      if (!(wsrc instanceof RemoteWebElement) ||
          !(wdst instanceof RemoteWebElement)) {
         this.reporter.ReportError("src/dst is not a RemoteWebElement!");
      }

      src = (RemoteWebElement)wsrc;
      dst = (RemoteWebElement)wdst;

      Thread.currentThread();

      Point srcPoint = src.getCoordinates().onScreen();
      Point dstPoint = dst.getCoordinates().onScreen();
      srcDim = src.getSize();
      dstDim = dst.getSize();
      src_x = srcPoint.getX() + (srcDim.getWidth() / 2);
      src_y = srcPoint.getY() + (srcDim.getHeight() / 2);
      dst_x = dstPoint.getX() + (dstDim.getWidth() / 2);
      dst_y = dstPoint.getY() + (dstDim.getHeight() / 2);

      this.reporter.Log(String.format("DnD Source Screen Coordinates: X => '%d' Y => '%d'", src_x, src_y));
      this.reporter.Log(String.format("DnD Dest Screen Coordinates: X => '%d' Y => '%d'", dst_x, dst_y));

      this.robo.mouseMove(srcPoint.x / 2, srcPoint.y / 2);
      this.robo.delay(800);
      this.robo.mousePress(InputEvent.BUTTON1_MASK);
      this.robo.delay(800);
      this.Move(src_x, src_y, dst_x, dst_y);
      this.robo.delay(800);
      this.robo.mouseRelease(InputEvent.BUTTON1_MASK);
      this.robo.delay(500);

      this.reporter.Log("DnD Finished.");
   }

   private void Move(int cur_x, int cur_y, int x, int y) {
      int x_count = 0;
      int y_count = 0;

      Thread.currentThread();

      if (cur_x < x) {
         for (x_count = cur_x; x_count <= x; x_count++) {
            this.robo.mouseMove(x_count, cur_y);
            this.robo.delay(3);
         }
      } else {
         for (x_count = cur_x; x <= x_count; x_count--) {
            this.robo.mouseMove(x_count, cur_y);
            this.robo.delay(3);
         }
      }

      if (cur_y < y) {
         for (y_count = cur_y; y_count <= y; y_count++) {
            this.robo.mouseMove(x_count, y_count);
            this.robo.delay(3);
         }
      } else {
         for (y_count = cur_y; y <= y_count; y_count--) {
            this.robo.mouseMove(x_count, y_count);
            this.robo.delay(3);
         }
      }
   }
}
