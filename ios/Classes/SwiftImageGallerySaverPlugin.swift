import Flutter
import UIKit
import Photos

public class SwiftImageGallerySaverPlugin: NSObject, FlutterPlugin {
    var result: FlutterResult?;
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "image_gallery_saver", binaryMessenger: registrar.messenger())
        let instance = SwiftImageGallerySaverPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        self.result = result
        if call.method == "saveImageToGallery" {
            guard let imageData = (call.arguments as? FlutterStandardTypedData)?.data,let image = UIImage(data: imageData) else {return}
            saveImage(image: image) { (path:String?) in
                 self.result?(path)
            }
//            UIImageWriteToSavedPhotosAlbum(image, self, #selector(didFinishSavingImage(image:error:contextInfo:)), nil)
            
            
        } else if (call.method == "saveFileToGallery") {
            guard let path = call.arguments as? String else { return }
            if (isImageFile(filename: path)) {
                if let image = UIImage(contentsOfFile: path) {
                    saveImage(image: image) { (path:String?) in
                        self.result?(path)
                    }
//                    UIImageWriteToSavedPhotosAlbum(image, self, #selector(didFinishSavingImage(image:error:contextInfo:)), nil)
                }
            } else {
                if (UIVideoAtPathIsCompatibleWithSavedPhotosAlbum(path)) {
                    saveVideo(videoPath: path) { (path:String?) in
                        self.result?(path)
                    }
//
//                    UISaveVideoAtPathToSavedPhotosAlbum(path, self, #selector(didFinishSavingVideo(videoPath:error:contextInfo:)), nil)
                }
            }
        } else {
            result(FlutterMethodNotImplemented)
        }
    }

    func saveImage(image:UIImage,handler: @escaping  ((String?) -> Void)){
        var localId:String?
        PHPhotoLibrary.shared().performChanges({
            let  assetChangeRequest=PHAssetChangeRequest.creationRequestForAsset(from: image)
            localId=assetChangeRequest.placeholderForCreatedAsset?.localIdentifier
        }) { (success:Bool, err:Error?) in
            if(!success){
                let reason="Error saving image: \(String(describing: err?.localizedDescription))"
                print(reason)
                handler(nil)
            }else if(localId != nil){
                let assetResult=PHAsset.fetchAssets(withLocalIdentifiers: [localId!], options: nil)
                let asset = assetResult.firstObject
                
                PHImageManager.default().requestImageData(for: asset!, options: nil) { (imageData:Data?, dataUTI:String?, orientation:UIImageOrientation, info:[AnyHashable : Any]?) in
                    print("\(String(describing: info?.keys))")
                    if let key = info?["PHImageFileUTIKey"] as? String {
                        print("path=====>\(String(describing: key))")
                        let fileUrl = URL(fileURLWithPath: key)
                        let path = "\(fileUrl.relativePath)";
                        print(path)
                        handler(path)
                    }else{
                        let reason="Error retrieving image filePath, heres whats available: \(String(describing: info))"
                        print(reason)
                        handler(nil)
                    }
                }
            }else{
                handler(nil)
            }
        }
    }
   
    
    func saveVideo(videoPath:String,handler: @escaping  ((String?) -> Void)){
        var localId:String?
        PHPhotoLibrary.shared().performChanges({
            let  assetChangeRequest=PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: URL(fileURLWithPath: videoPath))
            
            localId=assetChangeRequest?.placeholderForCreatedAsset?.localIdentifier
        }) { (success:Bool, err:Error?) in
            if(!success){
                let reason="Error saving video: \(String(describing: err?.localizedDescription))"
                print(reason)
                handler(nil)
            }else if(localId != nil){
                let assetResult=PHAsset.fetchAssets(withLocalIdentifiers: [localId!], options: nil)
                let asset = assetResult.firstObject
               
                PHImageManager.default().requestPlayerItem(forVideo: asset!, options: nil, resultHandler: { (videoData:AVPlayerItem?,info:[AnyHashable : Any]?) in
                       print("\(String(describing: info?.keys))")
                    if let key = info?["PHImageFileUTIKey"] as? String {
                        print("path=====>\(String(describing: key))")
                        let fileUrl = URL(fileURLWithPath: key)
                        let path = "\(fileUrl.relativePath)";
                        print(path)
                        handler(path)
                    }else{
                        let reason="Error retrieving video filePath, heres whats available: \(String(describing: info))"
                        print(reason)
                        handler(nil)
                    }
                })
               
            }else{
                handler(nil)
            }
        }
    }
    
    /// finish saving，if has error，parameters error will not nill
    @objc func didFinishSavingImage(image: UIImage, error: NSError?, contextInfo: UnsafeMutableRawPointer?) {
        
        result?(error == nil)
    }
    
    @objc func didFinishSavingVideo(videoPath: String, error: NSError?, contextInfo: UnsafeMutableRawPointer?) {
        result?(error == nil)
    }
    
    func isImageFile(filename: String) -> Bool {
        return filename.hasSuffix(".jpg")
            || filename.hasSuffix(".png")
            || filename.hasSuffix(".JPEG")
            || filename.hasSuffix(".JPG")
            || filename.hasSuffix(".PNG")
    }
}
