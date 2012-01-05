
#import <UIKit/UIKit.h>

@interface ViewController : UIViewController<UIWebViewDelegate> {
	IBOutlet UIWebView *_webView;
	NSTimer *_timer;
	int (*CTGetSignalStrength)();
	void *libHandle;
}

- (void)webViewDidFinishLoad:(UIWebView *)webView;
- (void)webViewDidStartLoad:(UIWebView *)webView;
- (void)timerFired:(NSTimer *)timer;
-(void)createReportCard;

@property (nonatomic, retain) UIWebView *webView;
@property (nonatomic, retain) NSTimer *timer;

@end
