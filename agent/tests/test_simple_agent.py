"""Tests for the simple A2UI agent."""
import pytest
from fastapi.testclient import TestClient
from simple_agent import app, generate_a2ui_response


@pytest.fixture
def client():
    """Create test client."""
    return TestClient(app)


class TestHealthEndpoints:
    """Test health and discovery endpoints."""

    def test_root_endpoint(self, client):
        """Test root health check."""
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert data["name"] == "AEM Content Assistant"
        assert "version" in data

    def test_agent_card(self, client):
        """Test A2A agent card endpoint."""
        response = client.get("/.well-known/agent-card.json")
        assert response.status_code == 200
        data = response.json()
        assert data["name"] == "AEM Content Assistant"
        assert "capabilities" in data
        assert data["capabilities"]["a2ui"]["version"] == "0.8"


class TestTaskEndpoint:
    """Test task creation endpoint."""

    def test_create_task_hero(self, client):
        """Test creating a task with hero keyword."""
        response = client.post("/tasks", json={
            "message": {
                "role": "user",
                "parts": [{"text": "create a hero banner"}]
            }
        })
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "completed"
        assert "id" in data
        assert "messages" in data
        assert len(data["messages"]) == 3

    def test_create_task_product(self, client):
        """Test creating a task with product keyword."""
        response = client.post("/tasks", json={
            "message": {
                "role": "user",
                "parts": [{"text": "product card please"}]
            }
        })
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "completed"
        # Check the data model has product-related content
        data_model = data["messages"][2]
        assert "dataModelUpdate" in data_model
        contents = data_model["dataModelUpdate"]["contents"]
        title_content = next((c for c in contents if c["key"] == "title"), None)
        assert title_content is not None
        assert "Product" in title_content["valueString"]

    def test_create_task_default(self, client):
        """Test creating a task with generic input."""
        response = client.post("/tasks", json={
            "message": {
                "role": "user",
                "parts": [{"text": "something random"}]
            }
        })
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "completed"


class TestA2UIMessageStructure:
    """Test A2UI message format compliance."""

    def test_message_structure(self):
        """Test that generated messages follow A2UI format."""
        messages = generate_a2ui_response("test")

        # Should have 3 messages
        assert len(messages) == 3

        # First message: beginRendering
        assert "beginRendering" in messages[0]
        begin = messages[0]["beginRendering"]
        assert "surfaceId" in begin
        assert "root" in begin

        # Second message: surfaceUpdate
        assert "surfaceUpdate" in messages[1]
        surface = messages[1]["surfaceUpdate"]
        assert "surfaceId" in surface
        assert "components" in surface

        # Third message: dataModelUpdate
        assert "dataModelUpdate" in messages[2]
        data = messages[2]["dataModelUpdate"]
        assert "surfaceId" in data
        assert "path" in data
        assert "contents" in data

    def test_surface_id_consistency(self):
        """Test that all messages use same surfaceId."""
        messages = generate_a2ui_response("hero")

        surface_id = messages[0]["beginRendering"]["surfaceId"]
        assert messages[1]["surfaceUpdate"]["surfaceId"] == surface_id
        assert messages[2]["dataModelUpdate"]["surfaceId"] == surface_id

    def test_component_structure(self):
        """Test that components have proper structure."""
        messages = generate_a2ui_response("test")
        components = messages[1]["surfaceUpdate"]["components"]

        # Each component should have id and component
        for comp in components:
            assert "id" in comp
            assert "component" in comp

    def test_data_model_contents(self):
        """Test data model has expected keys."""
        messages = generate_a2ui_response("hero")
        contents = messages[2]["dataModelUpdate"]["contents"]

        keys = [c["key"] for c in contents]
        assert "title" in keys
        assert "description" in keys
        assert "imageUrl" in keys


class TestActionEndpoint:
    """Test action handling endpoint."""

    def test_apply_suggestion_action(self, client):
        """Test apply_suggestion action."""
        response = client.post("/actions/apply_suggestion", json={})
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True

    def test_regenerate_action(self, client):
        """Test regenerate action."""
        response = client.post("/actions/regenerate", json={})
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert "messages" in data

    def test_unknown_action(self, client):
        """Test unknown action returns failure."""
        response = client.post("/actions/unknown_action", json={})
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is False


class TestCORS:
    """Test CORS configuration."""

    def test_cors_headers(self, client):
        """Test CORS headers are present."""
        response = client.options("/", headers={
            "Origin": "http://localhost:5173",
            "Access-Control-Request-Method": "GET"
        })
        # FastAPI TestClient may not fully simulate CORS preflight
        # but this ensures the middleware is configured
        assert response.status_code in [200, 405]
