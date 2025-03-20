# Forgejo Runner Setup for TetriStats

This directory contains the workflow configuration for building and releasing the TetriStats Android APK using Forgejo runners.

## Setting up a Forgejo Runner

To set up a Forgejo runner for this repository:

1. Navigate to your Forgejo instance and log in.

2. Go to your repository settings.

3. Navigate to "Actions" or "CI/CD" section.

4. Click on "Runners" and then "Add Runner."

5. Follow the instructions provided by Forgejo to register a new runner.

### Example runner registration commands:

```bash
# Download the Forgejo runner
curl -o forgejo-runner -L https://code.forgejo.org/forgejo/runner/releases/download/v3.3.0/forgejo-runner-3.3.0-linux-amd64
chmod +x forgejo-runner

# Register the runner with your Forgejo instance
./forgejo-runner register --no-interactive \
  --instance <your-forgejo-instance-url> \
  --token <your-registration-token> \
  --name <runner-name> \
  --labels ubuntu-latest,linux \
  --executor docker \
  --docker-image node:16 \
  --docker-volumes /var/run/docker.sock:/var/run/docker.sock

# Start the runner
./forgejo-runner daemon
```

Replace `<your-forgejo-instance-url>`, `<your-registration-token>`, and `<runner-name>` with your own values.

## Runner Configuration

The workflow is configured to:

1. Build a signed Android APK
2. Create a release with the APK attached when:
   - A new tag is pushed (for official releases)
   - A commit is pushed to main/master (for development releases)

## Troubleshooting

If you encounter issues with the runner:

1. Check the runner logs for errors
2. Ensure the runner has sufficient permissions to access your repository
3. Verify that your Forgejo instance supports actions similar to GitHub Actions
4. Make sure your runner environment has all necessary dependencies installed (Java 17, Android SDK, etc.)

For more information, refer to the [Forgejo Documentation](https://forgejo.org/docs/). 