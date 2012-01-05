
#import "ViewController.h"

#include "CoreTelephony.h"
#include <stdio.h>
#include <CoreFoundation/CoreFoundation.h>
#include <sys/time.h>
#include <dlfcn.h>

@implementation ViewController

@synthesize webView = _webView;
@synthesize timer = _timer;
@synthesize locationManager = _locationManager;
@synthesize data = _data;
@synthesize activeReportCard = _activeReportCard;

CFMachPortRef port;
struct CTServerConnection *sc=NULL;
struct CellInfo cellinfo;
int b;
int t1;

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Release any cached data, images, etc that aren't in use.
}

#pragma mark - View lifecycle

- (void)viewDidLoad
{
    [super viewDidLoad];
	[[UIApplication sharedApplication] setStatusBarStyle:UIStatusBarStyleBlackTranslucent animated:YES];
	
	self.locationManager = [[CLLocationManager alloc] init];
	self.locationManager.delegate = self;
    self.locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters;
    self.locationManager.distanceFilter = 500;
	
	[self.webView loadRequest:[NSURLRequest requestWithURL:[NSURL fileURLWithPath:[[NSBundle mainBundle] pathForResource:@"index" ofType:@"html"]isDirectory:NO]]];	
}

//void callback()
//{
//	printf("Callback called\n");
//}
//
//void cellconnect()
//{
//	int t1;
//	sc=_CTServerConnectionCreate(kCFAllocatorDefault, callback, NULL);
//	
//	/*
//	 port is not currently used, shuld be usable with a runloop.
//	 */
//	port=CFMachPortCreateWithPort(kCFAllocatorDefault, _CTServerConnectionGetPort(sc), NULL, NULL, NULL);
//	
//	_CTServerConnectionCellMonitorStart(&t1,sc);
//	
//	printf("Connected\n");
//}
//void getCellInfo()
//{
//	int cellcount;
//	_CTServerConnectionCellMonitorGetCellCount(&t1,sc,&cellcount);
//	
//	printf("Cell count: %x\n",cellcount);
//	
//	printf("Size = %x\n", sizeof(struct CellInfo));
//	
//	unsigned char *a=malloc(sizeof(struct CellInfo));
//	
//	for(b=0;b<cellcount;b++)
//	{       
//		//OMG the toolchain is broken, &cellinfo doesn't work
//		_CTServerConnectionCellMonitorGetCellInfo(&t1,sc,b,a); memcpy(&cellinfo,a,sizeof(struct CellInfo));
//		//OMG the toolchain is more broken, these printfs don't work on one line
//		printf("Cell Site: %d, MNC: %d, ",b,cellinfo.servingmnc);
//		printf("Location: %d, Cell ID: %d, Station: %d, ",cellinfo.location, cellinfo.cellid, cellinfo.station);
//		printf("Freq: %d, RxLevel: %d, ", cellinfo.freq, cellinfo.rxlevel);
//		printf("C1: %d, C2: %d\n", cellinfo.c1, cellinfo.c2);
//	}
//	if(a) free(a);
//	
//	void *libHandle = dlopen("/System/Library/Frameworks/CoreTelephony.framework/CoreTelephony", RTLD_LAZY);
//	int (*CTGetSignalStrength)();
//	CTGetSignalStrength = dlsym(libHandle, "CTGetSignalStrength");
//	if( CTGetSignalStrength == NULL) NSLog(@"Could not find CTGetSignalStrength");  
//	int result = CTGetSignalStrength();
//	printf("CTGetSignalStrength = %d\n", result);
//	dlclose(libHandle);
//	
//}


- (void)viewDidUnload
{
    [super viewDidUnload];
	dlclose(libHandle);
    // Release any retained subviews of the main view.
    // e.g. self.myOutlet = nil;
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
}

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated
{
	[super viewWillDisappear:animated];
}

- (void)viewDidDisappear:(BOOL)animated
{
	[super viewDidDisappear:animated];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    // Return YES for supported orientations
	if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone) {
	    return (interfaceOrientation != UIInterfaceOrientationPortraitUpsideDown);
	} else {
	    return YES;
	}
}

- (void)webViewDidFinishLoad:(UIWebView *)webView {
	libHandle = dlopen("/System/Library/Frameworks/CoreTelephony.framework/CoreTelephony", RTLD_LAZY);
	CTGetSignalStrength = dlsym(libHandle, "CTGetSignalStrength");
	if( CTGetSignalStrength == NULL) NSLog(@"Could not find CTGetSignalStrength");  
	
	self.timer = [NSTimer scheduledTimerWithTimeInterval:1.0 target:self selector:@selector(timerFired:) userInfo:nil repeats:YES];
}

- (void)webViewDidStartLoad:(UIWebView *)webView {     
}

-(void)timerFired:(NSTimer *)timer {	
	int signalStrength = CTGetSignalStrength();
	NSString *signalStrengthStr = [NSString stringWithFormat:@"%d", signalStrength];
	
	NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
	[dateFormatter setTimeStyle:NSDateFormatterLongStyle];
	[dateFormatter setDateStyle:NSDateFormatterMediumStyle];
	NSString *fireDateStr = [dateFormatter stringFromDate:[timer fireDate]];

	if(data == nil) {		
		data = [NSMutableDictionary dictionaryWithObjectsAndKeys:@"0", @"id",fireDateStr, @"last_updated", nil];
	}
	[data setObject:signalStrengthStr forKey:@"gsm_rssi"];
	[data setObject:fireDateStr forKey:@"last_updated"];

	NSError *error = nil;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:data options:NSJSONWritingPrettyPrinted error:&error];
	
    NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
	[self.webView stringByEvaluatingJavaScriptFromString:[NSString stringWithFormat:@"GuardedPanoptesCardUpdated(%@);", jsonString]];
	
	if(signalStrength < 25 || activeReportCard != nil) {
		
		if(activeReportCard == nil) {
			activeReportCard = [data mutableCopy];
			[activeReportCard removeObjectForKey:@"gsm_rssi"];
		}
		
		[activeReportCard setObject:@"1" forKey:@"id"];
		
		NSMutableArray *signalStrengths = [activeReportCard valueForKey:@"gsm_rssi"];
		if(signalStrengths == nil) {
			signalStrengths = [NSMutableArray arrayWithObject:signalStrengthStr];
		} else {
			[signalStrengths addObject:signalStrengthStr];
		}
		
		[activeReportCard setObject:signalStrengths forKey:@"gsm_rssi"];
			
		//		[activeReportCard setObject:signalStrengthStr forKey:@"gsm_rssi"];
		[activeReportCard setObject:fireDateStr forKey:@"last_updated"];
		
		[self.locationManager startMonitoringSignificantLocationChanges];
		
		jsonData = [NSJSONSerialization dataWithJSONObject:activeReportCard options:NSJSONWritingPrettyPrinted error:&error];
		jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];

		[self.webView stringByEvaluatingJavaScriptFromString:[NSString stringWithFormat:@"GuardedPanoptesCardUpdated(%@);", jsonString]];
	}
}

- (void)locationManager:(CLLocationManager *)manager
    didUpdateToLocation:(CLLocation *)newLocation
		   fromLocation:(CLLocation *)oldLocation
{
	if(activeReportCard != nil) {
		NSDate* eventDate = newLocation.timestamp;
		NSTimeInterval howRecent = [eventDate timeIntervalSinceNow];

		NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
		[dateFormatter setTimeStyle:NSDateFormatterLongStyle];
		[dateFormatter setDateStyle:NSDateFormatterMediumStyle];
		NSString *eventDateStr = [dateFormatter stringFromDate:eventDate];

		CLLocationCoordinate2D loc = [newLocation coordinate];
		NSString *latLongStr = [NSString stringWithFormat:@"%f,%f@%d", loc.latitude, loc.longitude, newLocation.horizontalAccuracy];

		[activeReportCard setObject:latLongStr forKey:@"loc"];
		[activeReportCard setObject:eventDateStr forKey:@"last_updated"];

		NSError *error = nil;
		
		NSData *jsonData = [NSJSONSerialization dataWithJSONObject:activeReportCard options:NSJSONWritingPrettyPrinted error:&error];
		
		NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
		[self.webView stringByEvaluatingJavaScriptFromString:[NSString stringWithFormat:@"GuardedPanoptesCardUpdated(%@);", jsonString]];

		NSLog(@"latitude %+.6f, longitude %+.6f\n",
			  newLocation.coordinate.latitude,
			  newLocation.coordinate.longitude);

		if (abs(howRecent) < 15.0)
		{
		}
	}
    // else skip the event and process the next one.
}

@end
