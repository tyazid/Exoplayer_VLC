##Update ExoPlayerDemo with VLC 

VLC Track Renderer Builder used by Exoplayer Demo to provide tracks renderers for audio/video content presenting.

- Add com.exovlc.demo to your ExoPlayerDemo project.
- Ensure that the VLC track builder is used a whole or a specific content type.

something like this:

```
//...
 case DemoUtil.TYPE_RAW_HTTP_MP4:
try {
		Properties p  = new Properties();
		p.put(com.exovlc.demo.VLCRendererBuilder.SURFACE_VIEW_RES_ID_PROP,  Integer.valueOf(R.id.surface_view) );
		return  new com.exovlc.demo.VLCRendererBuilder(this,   contentUri ,p);
		} catch (ExoPlaybackException e) {
			e.printStackTrace();
		}
		
```
enjoy.
