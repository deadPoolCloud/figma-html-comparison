# 📦 Guide: Publishing to GitHub

This guide will help you publish your Figma HTML Visual Regression Testing Tool to GitHub.

## Step 1: Stage All Files

Add all files (including new and modified) to the staging area:

```bash
git add .
```

Or if you want to add specific files:
```bash
git add README.md
git add src/
git add pom.xml
git add .gitignore
```

## Step 2: Commit Changes

Create your first commit:

```bash
git commit -m "Initial commit: Figma HTML Visual Regression Testing Tool

- Figma API integration for design fetching
- Selenium WebDriver for HTML page capture
- OpenCV-based pixel-to-pixel comparison
- Visual difference classification (alignment, spacing, font, color, missing elements)
- HTML report generation with QA-friendly observations
- CLI and REST API interfaces
- Responsive viewport support (Desktop/Tablet/Mobile)
- Complete documentation and examples"
```

## Step 3: Create GitHub Repository

1. **Go to GitHub.com** and sign in
2. **Click the "+" icon** in the top right corner
3. **Select "New repository"**
4. **Fill in the details:**
   - Repository name: `figma-html-visual-mirror` (or your preferred name)
   - Description: `QA-friendly Visual Regression Testing Tool comparing Figma designs with HTML implementations`
   - Visibility: Choose **Public** or **Private**
   - **DO NOT** initialize with README, .gitignore, or license (we already have these)
5. **Click "Create repository"**

## Step 4: Add Remote and Push

After creating the repository, GitHub will show you commands. Use these:

```bash
# Add the remote repository (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/figma-html-visual-mirror.git

# Rename branch to 'main' if needed (GitHub default is 'main')
git branch -M main

# Push to GitHub
git push -u origin main
```

**Note:** If your default branch is already `master`, use:
```bash
git remote add origin https://github.com/YOUR_USERNAME/figma-html-visual-mirror.git
git push -u origin master
```

## Step 5: Verify Upload

1. Go to your repository page on GitHub
2. Verify all files are present:
   - README.md
   - pom.xml
   - src/ directory with all Java files
   - .gitignore
   - Example usage scripts

## Optional: Add Repository Topics and Description

1. Go to your repository page
2. Click the ⚙️ gear icon next to "About"
3. Add topics: `visual-regression-testing`, `figma`, `selenium`, `opencv`, `qa-automation`, `java`
4. Add website URL if applicable
5. Click "Save changes"

## Quick Command Summary (All-in-One)

```bash
# Navigate to project directory (if not already there)
cd c:\Users\patha\IdeaProjects\figma-html-visual-mirror

# Stage all files
git add .

# Commit
git commit -m "Initial commit: Figma HTML Visual Regression Testing Tool"

# Add remote (replace YOUR_USERNAME)
git remote add origin https://github.com/YOUR_USERNAME/figma-html-visual-mirror.git

# Push to GitHub
git branch -M main  # or skip if using 'master'
git push -u origin main  # or 'master' if that's your branch name
```

## Troubleshooting

### If you get "remote origin already exists"
```bash
# Remove existing remote
git remote remove origin

# Add the correct remote
git remote add origin https://github.com/YOUR_USERNAME/figma-html-visual-mirror.git
```

### If you get authentication errors
- Use **Personal Access Token** instead of password
- Generate token: GitHub → Settings → Developer settings → Personal access tokens → Generate new token
- Select scopes: `repo` (full control of private repositories)
- Use the token as password when prompted

### If you want to use SSH instead of HTTPS
```bash
# Add SSH remote
git remote add origin git@github.com:YOUR_USERNAME/figma-html-visual-mirror.git

# Push
git push -u origin main
```

## Next Steps After Publishing

1. **Add License** (optional but recommended)
   - Go to repository → Add file → Create new file
   - Name it `LICENSE`
   - Choose a license (MIT, Apache 2.0, etc.)

2. **Add Badges** (optional)
   - Add build status, version badges to README.md

3. **Create Releases**
   - Go to Releases → Create a new release
   - Tag version: `v1.0.0`
   - Add release notes

4. **Enable Issues** (if you want community contributions)
   - Repository → Settings → Features → Enable Issues

---

**🎉 Congratulations! Your project is now on GitHub!**
