import java.io.IOException;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

public class Seg {
    public static void main(String[] args) {
        String BASE_URL = "<BASE_URL>";
        String FILE_SERVER_URL = "<FILE_SERVER_URL>";
        String ZH_TOKEN = "<ZH_TOKEN>";
        String USER_ID = "<USER_ID>";

        String FILE_PATH = "l.stl"; // STL file
        String JAW_TYPE = "Lower"; // Upper for upper jaw, Lower for lower jaw

        try{
            HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

            long now = System.currentTimeMillis();

            // Step 1. Upload mesh to server
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(
                    FILE_SERVER_URL + "/scratch/APIClient/" +
                    USER_ID + "/upload_url?postfix=stl")
                )
                .headers("X-ZH-TOKEN", ZH_TOKEN)
                .GET()
                .build();

            HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

            String upload_url = response.body();
            upload_url = upload_url.substring(1, upload_url.length()-1);

            String stl_urn = "urn:zhfile:o:s:APIClient:"+ USER_ID+ ":" +
                upload_url.substring(
                    upload_url.indexOf(USER_ID) + USER_ID.length() + 1,
                    upload_url.indexOf("?"));

            request = HttpRequest.newBuilder()
                .uri(URI.create(upload_url))
                .PUT(HttpRequest.BodyPublishers.ofFile(Paths.get(FILE_PATH)))
                .build();

            response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Upload takes " +
                ((System.currentTimeMillis() - now)/ 1000.0) +
                " seconds" );

            // Step 2. Launch job
            JSONObject job_request = new JSONObject();
            JSONObject mesh_input = new JSONObject();
            JSONObject input_data = new JSONObject();
            JSONObject mesh_output_config = new JSONObject();
            JSONObject output_config = new JSONObject();

            mesh_output_config.put("type", "stl");
            output_config.put("mesh", mesh_output_config);
            mesh_input.put("type", "stl");
            mesh_input.put("data", stl_urn);
            input_data.put("mesh", mesh_input);
            input_data.put("jaw_type", JAW_TYPE);
            job_request.put("spec_group", "mesh-processing");
            job_request.put("spec_name", "oral-seg");
            job_request.put("spec_version", "1.0-snapshot");
            job_request.put("user_id", USER_ID);
            job_request.put("user_group", "APIClient");
            job_request.put("input_data", input_data);
            job_request.put("output_config", output_config);


            request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/run"))
                .header("Content-Type", "application/json")
                .header("accept", "application/json")
                .header("X-ZH-TOKEN", ZH_TOKEN)
                .POST(HttpRequest.BodyPublishers.ofString(job_request.toString()))
                .build();

            response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json_response = new JSONObject(response.body());
            String run_id = json_response.get("run_id").toString();
            System.out.println("run id is: " + run_id);

            // Step 3. wait until job finish

            request = HttpRequest.newBuilder()
                .uri(new URI(
                    BASE_URL + "/run/" + run_id)
                )
                .headers("X-ZH-TOKEN", ZH_TOKEN)
                .header("accept", "application/json")
                .GET()
                .build();

            now = System.currentTimeMillis();

            while (true) {
                TimeUnit.SECONDS.sleep(3);
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                json_response = new JSONObject(response.body());
                if(json_response.getBoolean("failed")) throw new Exception(
                    "Job Errored: " + json_response.get("reason_public").toString());
                if(json_response.getBoolean("completed")) break;
            }

            System.out.println("Job completed in " +
                ((System.currentTimeMillis() - now)/ 1000.0) +
                " seconds" );

            // Step 4. Get results
            request = HttpRequest.newBuilder()
                .uri(new URI(
                    BASE_URL + "/data/" + run_id)
                )
                .headers("X-ZH-TOKEN", ZH_TOKEN)
                .header("accept", "application/json")
                .GET()
                .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            json_response = new JSONObject(response.body());
            JSONArray arr = json_response.getJSONArray("seg_labels");
            FileWriter f_writer = new FileWriter("seg_labels.txt");

            for (int i = 0; i < arr.length(); i++) {
                f_writer.write(arr.getInt(i) + "\n");
            }

            f_writer.close();

            // Step 5. Download result mesh from file server
            request = HttpRequest.newBuilder()
                .uri(new URI(
                    FILE_SERVER_URL + "/file/download?urn=" +
                    ((JSONObject) json_response.get("mesh")).get("data").toString())
                )
                .headers("X-ZH-TOKEN", ZH_TOKEN)
                .GET()
                .build();
            HttpResponse<Path> response_download =
                client.send(request, HttpResponse.BodyHandlers.ofFile(Paths.get("processed_mesh.stl")));

            System.out.println("Completed: Mesh saved to processed_mesh.stl and Label saved to seg_labels.txt");
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println(e);
            System.out.println("job errored.");
            return;
        }
    }
}