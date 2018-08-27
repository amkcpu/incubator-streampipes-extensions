/*
 * Copyright 2018 FZI Forschungszentrum Informatik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.streampipes.processors.imageprocessing.jvm.processor.imagecropper;

import org.streampipes.hmi.jvm.processor.commons.ImageTransformer;
import org.streampipes.hmi.jvm.processor.imageenrichment.BoxCoordinates;
import org.streampipes.model.graph.DataProcessorInvocation;
import org.streampipes.wrapper.routing.SpOutputCollector;
import org.streampipes.wrapper.standalone.engine.StandaloneEventProcessorEngine;

import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ImageCropper extends StandaloneEventProcessorEngine<ImageCropperParameters> {

  private ImageCropperParameters params;

  public ImageCropper(ImageCropperParameters params) {
    super(params);
  }

  @Override
  public void onInvocation(ImageCropperParameters imageCropperParameters, DataProcessorInvocation dataProcessorInvocation) {
    this.params = imageCropperParameters;
  }

  @Override
  public void onEvent(Map<String, Object> in, String s, SpOutputCollector out) {
    ImageTransformer imageTransformer = new ImageTransformer(in, params);
    Optional<BufferedImage> imageOpt = imageTransformer.getImage();

    if (imageOpt.isPresent()) {
      BufferedImage image = imageOpt.get();
      BoxCoordinates boxCoordinates = imageTransformer.getBoxCoordinates(image);

      BufferedImage dest = image.getSubimage(boxCoordinates.getX(), boxCoordinates.getY(), boxCoordinates.getWidth(),
              boxCoordinates.getHeight());

      Optional<byte[]> finalImage = imageTransformer.makeImage(dest);

      if (finalImage.isPresent()) {
        Map<String, Object> outMap = new HashMap<>();
        outMap.put("image", Base64.getEncoder().encodeToString(finalImage.get()));
        out.onEvent(outMap);
      }
    }
  }

  @Override
  public void onDetach() {

  }
}