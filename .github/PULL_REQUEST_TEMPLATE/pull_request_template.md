---
name: Pull request
about: Request to have code merged into FreePaint
title: "FreePaint Pull Request"
labels: ''
assignees: ''

---

**Checklist**  
In order for your pull request to be considered, please follow the checklist below. Ensure you have not deleted any points but instead checked them by replacing `[ ]` with `[x]` or clicking on the boxes.

- [ ] I have given my pull request a descriptive title, written in present tense. It begins with a verb and is not vague.
    - For instance, a bad title would be "Make the menu change." Which menu? What do you define as a "changing menu"? Perhaps a better name would be "Change settings title based on tool name".
- [ ] My pull request contains small, regular commits, named to the same standard as the PR itself.
    - A good rule of thumb is if you introduce a Java class file, or multiple files, that your PR needs separate commits. I need to see your thinking in action so it's easier to follow and verify.
- [ ] My pull request focuses on one feature or bug.
    - A common mistake can be made when one uses the same fork for multiple pull requests. Make sure your PR has only the commits you need to merge your specific feature.
- [ ] My PR is based off of and is merging with the branch for the latest development version code of FreePaint (for instance, if the latest stable version is v1.5.0, instead of merging to `main`, I am merging to `1.6.0`.
    - If such a branch does not exist, please notify the maintainer.
- [ ] If my PR introduces a feature, I explain in detail what this feature does.
    - Try to include screenshots or even videos. You have to explain what features you are planning on adding so that the maintainer or community members don't have to run your PR to understand your goal.
    - If you're changing a menu, for instance, include screenshots of what you've changed.


**Make sure to open an issue first, and get acknowledgement from the maintainer and/or community members! Your PR might take the project in an entirely different direction than the maintainer intends, so you don't want to waste all your work.**
