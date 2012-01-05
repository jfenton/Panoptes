
#import <UIKit/UIKit.h>
#import <CoreLocation/CLLocationManager.h>
#import <CoreLocation/CLLocationManagerDelegate.h>

@interface ViewController : UIViewController<UIWebViewDelegate, CLLocationManagerDelegate> {
	IBOutlet UIWebView *_webView;
	CLLocationManager *_locationManager;
	NSTimer *_timer;
	NSMutableDictionary *data;
	int (*CTGetSignalStrength)();
	void *libHandle;
}

- (void)webViewDidFinishLoad:(UIWebView *)webView;
- (void)webViewDidStartLoad:(UIWebView *)webView;
- (void)timerFired:(NSTimer *)timer;

@property (nonatomic, retain) NSMutableDictionary *data;
@property (nonatomic, retain) UIWebView *webView;
@property (nonatomic, retain) NSTimer *timer;
@property (nonatomic, retain) CLLocationManager *locationManager;

@end
