package com.qp.quantum_share.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;
import org.springframework.web.multipart.MultipartFile;

import com.qp.quantum_share.configuration.ConfigurationClass;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.MediaPost;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.exception.FBException;
import com.qp.quantum_share.response.ErrorResponse;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.SuccessResponse;
import com.restfb.BinaryAttachment;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.types.FacebookType;
import com.restfb.types.GraphResponse;
import com.restfb.types.ResumableUploadStartResponse;
import com.restfb.types.ResumableUploadTransferResponse;

@RestController
public class TestClass {
	
	@Autowired
	ConfigurationClass config;
	
	@Autowired
	ResponseStructure<String> structure;
	
	@Autowired
	QuantumShareUserDao userDao;
	
	@PostMapping("/test")
	public ResponseEntity<List<Object>> postMediaToPage(MediaPost mediaPost, MultipartFile mediaFile
			) {
		List<Object> mainresponse = config.getList();
		mainresponse.clear();
		try {
//			List<FacebookPageDetails> pages = user.getPageDetails();
//			if (pages.isEmpty()) {
//				structure.setCode(HttpStatus.NOT_FOUND.value());
//				structure.setMessage("No pages are available for this Facebook account.");
//				structure.setPlatform("facebook");
//				structure.setStatus("error");
//				structure.setData(null);
//				mainresponse.add(structure);
//				return new ResponseEntity<List<Object>>(mainresponse, HttpStatus.NOT_FOUND);
//			}
//			for (FacebookPageDetails page : pages) {
				String facebookPageId = "260335560496500";
				String pageAccessToken = "EAAT2rtm6eagBOZCOaaMmZCrR4aNICeZCJ9mIG9ZCMZCczLN9BOsRZBm7MOWJ8AI74TOSYBtvAprzX405hdOpBvCAT0zNqAij6k00069VDXTQCpiOzvTB3wtsLd3oWkQ37EJIR0qzJUIrgTvJ1OigrhD7XkewaRh656g3JjX7GsMwVHhJnbPlgWZC8A9CQSP2xh6CZCPIqnyROkNCoLCqAjrv4abgLBcZD";

				FacebookClient client = config.getFacebookClient(pageAccessToken);

				FacebookType response;
				if (isVideo(mediaFile)) {
					byte[] videoByte = mediaFile.getBytes();
					int videosize = videoByte.length;
					String uploadSessionId = createVideoUploadSession(client, facebookPageId, videosize);
					uploadSessionId = uploadSessionId.replaceAll("\"", "");
					long startOffset = 0;

					while (startOffset < videosize) {
						startOffset = uploadVideoChunk(client, facebookPageId, uploadSessionId, startOffset, videoByte);
					}
					GraphResponse finalResponse = finishVideoUploadSession(facebookPageId, client, uploadSessionId,
							mediaPost.getCaption());
					
					if (finalResponse.isSuccess()) {
						
						SuccessResponse succesresponse = config.getSuccessResponse();
						succesresponse.setCode(HttpStatus.OK.value());
						succesresponse.setMessage("Posted On FaceBook Page");
						succesresponse.setStatus("success");
						succesresponse.setPlatform("facebook");
						succesresponse.setData(finalResponse);
						mainresponse.add(succesresponse);
					} else {
						ErrorResponse errResponse = config.getErrorResponse();
						errResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
						errResponse.setMessage("Request Failed to post on " );
						errResponse.setStatus("error");
						errResponse.setPlatform("facebook");
						errResponse.setData(finalResponse);
						mainresponse.add(errResponse);
					}

				} else {
					response = client.publish(facebookPageId + "/photos", FacebookType.class,
							BinaryAttachment.with("source", mediaFile.getBytes()),
							Parameter.with("message", mediaPost.getCaption()),
							Parameter.with("published", true),
							Parameter.with("scheduled_publish_time", 1720526447));
					System.out.println(response);
					if (response.getId() != null) {
						System.out.println(response);
						SuccessResponse succesresponse = config.getSuccessResponse();
//						qsuser.setCredit(qsuser.getCredit() - 1);
//						userDao.save(qsuser);
//						System.out.println("facebook : " + qsuser +" "+ LocalTime.now());
						succesresponse.setCode(HttpStatus.OK.value());
						succesresponse.setMessage("Posted On FaceBook Page");
						succesresponse.setStatus("success");
						succesresponse.setData(response);
						succesresponse.setPlatform("facebook");
//						System.out.println("facebook " + qsuser.getCredit());
//						succesresponse.setRemainingCredits(qsuser.getCredit());
						mainresponse.add(succesresponse);
					} else {
						ErrorResponse errResponse = config.getErrorResponse();
						errResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
						errResponse.setMessage("Request Failed to post on ");
						errResponse.setStatus("error");
						errResponse.setData(response);
						errResponse.setPlatform("facebook");
						mainresponse.add(errResponse);
					}
				}
//			}
			return new ResponseEntity<List<Object>>(mainresponse, HttpStatus.OK);

		} catch (FacebookException e) {
			throw new FBException(e.getMessage(), "facebook");
		} catch (IllegalArgumentException e) {
			throw new CommonException(e.getMessage());
		} catch (IOException e) {
			throw new CommonException(e.getMessage());
		} catch (NullPointerException e) {
			throw new NullPointerException(e.getMessage());
		} catch (InternalServerError error) {
			throw new CommonException(error.getMessage());
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}

	}

	// post video
		public String createVideoUploadSession(FacebookClient client, String pageId, long fileSize) {
			ResumableUploadStartResponse response = client.publish(pageId + "/videos", ResumableUploadStartResponse.class,
					Parameter.with("upload_phase", "start"), Parameter.with("file_size", fileSize));
			return response.getUploadSessionId();
		}

		public Long uploadVideoChunk(FacebookClient client, String facebookPageId, String uploadSessionId, long startOffset,
				byte[] vidFile) {
			ResumableUploadTransferResponse response = client.publish(facebookPageId + "/videos",
					ResumableUploadTransferResponse.class, BinaryAttachment.with("video_file_chunk", vidFile),
					Parameter.with("upload_phase", "transfer"), Parameter.with("start_offset", startOffset),
					Parameter.with("upload_session_id", uploadSessionId));
			return response.getStartOffset();
		}

		public GraphResponse finishVideoUploadSession(String facebookPageId, FacebookClient client, String uploadSessionId,
				String message) {
			GraphResponse response = client.publish(facebookPageId + "/videos", GraphResponse.class,
					Parameter.with("upload_phase", "finish"), Parameter.with("upload_session_id", uploadSessionId),
					Parameter.with("description", message));
			return response;
		}

		public boolean isVideo(MultipartFile file) {
			if (file.getContentType().startsWith("video")) {
				return true;
			} else if (file.getContentType().startsWith("image")) {
				return false;
			} else {
				throw new IllegalArgumentException("Unsupported file type: " + file.getContentType());
			}
		}

}
