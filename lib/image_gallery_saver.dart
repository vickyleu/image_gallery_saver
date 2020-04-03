import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

class ImageGallerySaver {
  static const MethodChannel _channel =
      const MethodChannel('image_gallery_saver');

  /// save image to Gallery
  /// imageBytes can't null
  static Future<String> saveImage(Uint8List imageBytes) async {
    assert(imageBytes != null);
    String result =
        await _channel.invokeMethod('saveImageToGallery', imageBytes);
    return result;
  }

  /// Save the PNG，JPG，JPEG image or video located at [file] to the local device media gallery.
  static Future<String> saveFile(String file) async {
    assert(file != null);
    String result = await _channel.invokeMethod('saveFileToGallery', file);
    return result;
  }
}
