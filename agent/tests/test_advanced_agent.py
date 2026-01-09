"""Tests for the advanced A2UI agent."""
import pytest
from fastapi.testclient import TestClient
from advanced_agent import app


@pytest.fixture
def client():
    """Create test client."""
    return TestClient(app)


class TestAdvancedRoot:
    """Test root endpoint."""

    def test_root(self, client):
        """Test root returns demo list."""
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert "demos" in data


class TestDynamicListDemo:
    """Test dynamic list demo."""

    def test_demo_list(self, client):
        """Test list demo endpoint."""
        response = client.get("/demo/list")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "completed"
        assert "messages" in data

    def test_list_has_template(self, client):
        """Test that list demo uses template pattern."""
        response = client.get("/demo/list")
        data = response.json()
        messages = data["messages"]

        # Find surfaceUpdate
        surface_update = next(
            (m for m in messages if "surfaceUpdate" in m),
            None
        )
        assert surface_update is not None

        # Check for List component with template
        components = surface_update["surfaceUpdate"]["components"]
        list_comp = next(
            (c for c in components if "List" in c.get("component", {})),
            None
        )
        assert list_comp is not None

    def test_list_has_data(self, client):
        """Test that list demo includes data model."""
        response = client.get("/demo/list")
        data = response.json()
        messages = data["messages"]

        # Find dataModelUpdate
        data_update = next(
            (m for m in messages if "dataModelUpdate" in m),
            None
        )
        assert data_update is not None


class TestWizardDemo:
    """Test wizard demo."""

    def test_demo_wizard_step1(self, client):
        """Test wizard step 1."""
        response = client.get("/demo/wizard")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "completed"

    def test_demo_wizard_step2(self, client):
        """Test wizard step 2."""
        response = client.get("/demo/wizard_2")
        assert response.status_code == 200

    def test_demo_wizard_step3(self, client):
        """Test wizard step 3."""
        response = client.get("/demo/wizard_3")
        assert response.status_code == 200

    def test_wizard_navigation(self, client):
        """Test wizard navigation action."""
        response = client.post("/actions/wizard_navigate", json={"step": 2})
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert "messages" in data


class TestTabsDemo:
    """Test tabs demo."""

    def test_demo_tabs(self, client):
        """Test tabs demo endpoint."""
        response = client.get("/demo/tabs")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "completed"

    def test_tabs_has_tab_component(self, client):
        """Test that tabs demo has Tabs component."""
        response = client.get("/demo/tabs")
        data = response.json()
        messages = data["messages"]

        surface_update = next(
            (m for m in messages if "surfaceUpdate" in m),
            None
        )
        assert surface_update is not None

        components = surface_update["surfaceUpdate"]["components"]
        tabs_comp = next(
            (c for c in components if "Tabs" in c.get("component", {})),
            None
        )
        assert tabs_comp is not None


class TestAIDemo:
    """Test AI content generator demo."""

    def test_demo_ai(self, client):
        """Test AI demo endpoint."""
        response = client.get("/demo/ai")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "completed"

    def test_generate_content_action(self, client):
        """Test generate content action."""
        response = client.post("/actions/generate_content", json={})
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True


class TestCollaborationDemo:
    """Test collaboration demo."""

    def test_demo_collab(self, client):
        """Test collaboration demo endpoint."""
        response = client.get("/demo/collab")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "completed"


class TestTasksEndpoint:
    """Test tasks endpoint with natural language."""

    def test_task_list_keyword(self, client):
        """Test task with list-related keyword."""
        response = client.post("/tasks", json={
            "message": {
                "parts": [{"text": "show me the asset browser"}]
            }
        })
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "completed"

    def test_task_wizard_keyword(self, client):
        """Test task with wizard-related keyword."""
        response = client.post("/tasks", json={
            "message": {
                "parts": [{"text": "help me create component step by step"}]
            }
        })
        assert response.status_code == 200

    def test_task_tabs_keyword(self, client):
        """Test task with tabs-related keyword."""
        response = client.post("/tasks", json={
            "message": {
                "parts": [{"text": "show properties tabs"}]
            }
        })
        assert response.status_code == 200


class TestUnknownDemo:
    """Test unknown demo handling."""

    def test_unknown_demo(self, client):
        """Test that unknown demo returns error."""
        response = client.get("/demo/nonexistent")
        assert response.status_code == 200
        data = response.json()
        # Should contain error info
        assert "error" in data
        assert "available" in data
